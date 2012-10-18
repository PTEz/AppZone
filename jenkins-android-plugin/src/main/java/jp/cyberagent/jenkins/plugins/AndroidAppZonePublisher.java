
package jp.cyberagent.jenkins.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

public class AndroidAppZonePublisher extends Notifier {
    public static final String DEFAULT_APPSERVER = "http://appzone-api.pes.ch/";

    private static final String TAG = "[AppZone] ";

    private final String id;
    private String name;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public AndroidAppZonePublisher(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public FormValidation doCheckId(@QueryParameter
    final String value)
            throws IOException, ServletException {
        if (value.length() == 0) {
            return FormValidation.error("ID needs to be set.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckName(@QueryParameter
    final String value)
            throws IOException, ServletException {
        if (value.length() == 0) {
            return FormValidation.error("Name needs to be set.");
        }
        return FormValidation.ok();
    }

    public FormValidation doCheckApkFile(@QueryParameter
    final String value)
            throws IOException, ServletException {
        if (value.length() == 0) {
            return FormValidation.error("ApkFile needs to be set.");
        }
        return FormValidation.ok();
    }

    @Override
    public boolean perform(final AbstractBuild build, final Launcher launcher,
            final BuildListener listener) {
        String server = getDescriptor().getServer();
        if (server == null || server.length() == 0) {
            listener.getLogger().println(TAG +
                    "NBU AppZone server not set. Please set in global config! Aborting.");
            return false;
        }

        try {
            DeployStrategy deploy = createDeployStrategy(build, listener);
            if (deploy == null) {
                return false;
            }
            HttpClient httpclient = new HttpClient();
            PostMethod filePost = new PostMethod(deploy.getUrl());
            if (name == null || name.length() == 0) {
                name = build.getProject().getName();
            }
            filePost.setRequestEntity(new MultipartRequestEntity(deploy.getParts(), filePost
                    .getParams()));
            listener.getLogger().println(TAG + "Publishing to: " + deploy.getUrl());
            httpclient.executeMethod(filePost);
            int statusCode = filePost.getStatusCode();
            if (statusCode < 200 || statusCode > 299) {
                String body = filePost.getResponseBodyAsString();
                listener.getLogger().println(TAG + "Response (" + statusCode + "):" + body);
                return false;
            }
            return true;
        } catch (IOException e) {
            listener.getLogger().print(e.getMessage());
        }
        return false;
    }

    private DeployStrategy createDeployStrategy(final AbstractBuild build,
            final BuildListener listener) throws FileNotFoundException {
        // get .apk and .ipa files
        Collection<File> files = getPossibleAppFiles(build);
        File ipaFile = getIpaFileThatHasManifest(files, build, listener);
        File apkFile = getApkFile(files);
        String version = getVersion(ipaFile, apkFile, build, listener);
        listener.getLogger().println(TAG + "Version: " + version);
        if (ipaFile != null) {
            listener.getLogger().println(TAG + "File: " + ipaFile.getAbsolutePath());
            return new IOsDeployStrategy(id, version, ipaFile, getManifestFileFor(ipaFile));
        } else if (apkFile != null) {
            listener.getLogger().println(TAG + "File: " + apkFile.getAbsolutePath());
            return new AndroidDeployStrategy(id, version, apkFile);
        } else {
            String errorMessage = TAG
                    + "Could not find a .apk file nor a .ipa with matching .manifest file. Aborting.";
            listener.getLogger().println(errorMessage);
            return null;
        }
    }

    private Collection<File> getPossibleAppFiles(final AbstractBuild build) {
        File dir = new File(build.getWorkspace().getRemote());
        Collection<File> files = FileUtils.listFiles(
                dir,
                new RegexFileFilter("(.(?!unaligned)(?!unsigned))*(\\.apk|\\.ipa)"),
                FileFilterUtils.makeCVSAware(DirectoryFileFilter.DIRECTORY)
                );
        return files;
    }

    private File getIpaFileThatHasManifest(final Collection<File> files, final AbstractBuild build,
            final BuildListener listener) {
        for (File file : files) {
            // && getManifestFileFor(file).exists()
            if (file.getAbsolutePath().endsWith(".ipa")) {
                Collection<File> plistFiles = FileUtils.listFiles(
                        new File(build.getWorkspace().getRemote()),
                        new AbstractFileFilter() {

                            @Override
                            public boolean accept(final File file) {
                                return file.getAbsolutePath().endsWith(".app/Info.plist");
                            }
                        },
                        FileFilterUtils.makeCVSAware(DirectoryFileFilter.DIRECTORY)
                        );
                if (plistFiles.isEmpty()) {
                    return null;
                } else if (plistFiles.size() > 1) {
                    listener.getLogger().println(
                            TAG + "Error: Found multiple Info.plist files in *.app folders.");
                    return null;
                }

                File manifestFile = getManifestFileFor(file);
                listener.getLogger().println(TAG + "Creating " + manifestFile.getAbsolutePath());
                try {
                    NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(
                            plistFiles.iterator().next());
                    String bundleName = rootDict.objectForKey("CFBundleName")
                            .toString();
                    String bundleIdentifier = rootDict.objectForKey("CFBundleIdentifier")
                            .toString();
                    String bundleShortVersion = rootDict.objectForKey("CFBundleShortVersionString")
                            .toString();

                    InputStream inputStream = getClass().getResourceAsStream(
                            "/OTAManifestTemplate.plist");
                    StringWriter writer = new StringWriter();
                    IOUtils.copy(inputStream, writer, "UTF-8");
                    String manifest = writer.toString();

                    manifest = manifest.replace("${url}", createAppUrl("ios", id));
                    manifest = manifest.replace("${CFBundleName}", bundleName);
                    manifest = manifest.replace("${CFBundleIdentifier}", bundleIdentifier);
                    manifest = manifest.replace("${CFBundleShortVersionString}",
                            bundleShortVersion);
                    FileUtils.writeStringToFile(manifestFile, manifest);
                } catch (Exception e) {
                    listener.getLogger().println(
                            TAG + "Problmen creating manifest based on Info.plist: "
                                    + e.getMessage());
                    return null;
                }
                return file;
            }
        }
        return null;
    }

    private File getApkFile(final Collection<File> files) {
        for (File file : files) {
            if (file.getName().endsWith(".apk")) {
                return file;
            }
        }
        return null;
    }

    private File getManifestFileFor(final File ipaFile) {
        String path = ipaFile.getAbsolutePath();
        return new File(path.substring(0, path.length() - 4) + ".manifest");
    }

    private String getVersion(final File ipaFile, final File apkFile, final AbstractBuild build,
            final BuildListener listener) {
        File versionFile = new File(build.getWorkspace().getRemote() + "/VERSION");
        if (versionFile.exists()) {
            try {
                FileInputStream fstream = new FileInputStream(versionFile);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String strLine = br.readLine();
                in.close();
                return strLine;
            } catch (Exception e) {
                listener.getLogger().println(TAG + "Error: " + e.getMessage());
            }
        } else if (ipaFile != null) {
            try {
                File file = getManifestFileFor(ipaFile);
                NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(file);
                NSArray items = (NSArray) rootDict.objectForKey("items");
                NSDictionary item = (NSDictionary) items.objectAtIndex(0);
                NSDictionary metadata = (NSDictionary) item.objectForKey("metadata");
                return metadata.objectForKey("bundle-version").toString();
            } catch (Exception e) {
                listener.getLogger().println(TAG + "Error: " + e.getMessage());
            }
        } else if (apkFile != null) {
            try {
                ZipFile zip = new ZipFile(apkFile);
                ZipEntry mft = zip.getEntry("AndroidManifest.xml");
                InputStream is = zip.getInputStream(mft);

                byte[] xml = new byte[is.available()];
                is.read(xml);

                String string = AndroidUtils.decompressXML(xml);
                int start = string.indexOf("versionName=\"") + 13;
                int end = string.indexOf("\"", start);
                String version = string.substring(start, end);
                if (!version.startsWith("resourceID")) {
                    return version;
                }
            } catch (Exception e) {
                listener.getLogger().println(TAG + "Error: " + e.getMessage());
            }
        }
        return "NOT SET";
    }

    private String createAppUrl(final String type, final String id) {
        String server = getDescriptor().getServer();
        StringBuilder url = new StringBuilder(server);
        if (!server.endsWith("/")) {
            url.append("/");
        }
        url.append("app/" + id + "/" + type);
        return url.toString();
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.BUILD;
    }

    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        private String server;

        public DescriptorImpl() {
            super(AndroidAppZonePublisher.class);
            load();
        }

        public FormValidation doCheckServer(@QueryParameter
        final String value)
                throws IOException, ServletException {
            if (value.length() != 0 && !value.startsWith("http")) {
                return FormValidation.error("Server needs to start with http");
            }
            return FormValidation.ok();
        }

        @Override
        public boolean isApplicable(final Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        @Override
        public String getDisplayName() {
            return "Publish to NBU AppZone";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject formData)
                throws FormException {
            req.bindJSON(this, formData);
            save();
            return super.configure(req, formData);
        }

        public String getServer() {
            if (server == null || server.length() == 0) {
                server = DEFAULT_APPSERVER;
            }
            return server;
        }

        public void setServer(final String server) {
            this.server = server;
        }
    }

    private abstract class DeployStrategy {
        private final String mUrl;

        public DeployStrategy(final String type, final String id) {
            mUrl = createAppUrl(type, id);
        }

        public String getUrl() {
            return mUrl;
        }

        public abstract Part[] getParts();
    }

    private class AndroidDeployStrategy extends DeployStrategy {

        private final Part[] mParts;

        public AndroidDeployStrategy(final String id, final String version, final File apkFile)
                throws FileNotFoundException {
            super("android", id);
            mParts = new Part[] {
                    new StringPart("version", version),
                    new FilePart("apk", apkFile),
            };
        }

        @Override
        public Part[] getParts() {
            return mParts;
        }
    }

    private class IOsDeployStrategy extends DeployStrategy {

        private final Part[] mParts;

        public IOsDeployStrategy(final String id, final String version,
                final File ipaFile, final File manifestFile) throws FileNotFoundException {
            super("ios", id);
            mParts = new Part[] {
                    new StringPart("version", version),
                    new FilePart("ipa", ipaFile),
                    new FilePart("manifest", manifestFile),
            };
        }

        @Override
        public Part[] getParts() {
            return mParts;
        }
    }
}
