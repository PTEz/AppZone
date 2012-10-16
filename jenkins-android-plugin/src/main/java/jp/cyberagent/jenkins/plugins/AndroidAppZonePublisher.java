
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

/**
 * Sample {@link Builder}.
 * <p>
 * When the user configures the project and enables this builder,
 * {@link DescriptorImpl#newInstance(StaplerRequest)} is invoked and a new
 * {@link HelloWorldBuilder} is created. The created instance is persisted to
 * the project configuration XML by using XStream, so this allows you to use
 * instance fields (like {@link #apkFile}) to remember the configuration.
 * <p>
 * When a build is performed, the
 * {@link #perform(AbstractBuild, Launcher, BuildListener)} method will be
 * invoked.
 * 
 * @author Kohsuke Kawaguchi
 */
public class AndroidAppZonePublisher extends Notifier {
    public static final String DEFAULT_APPSERVER = "http://172.19.4.248/appzone/";

    private final String id;
    private String name;
    private final String apkFile;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public AndroidAppZonePublisher(final String id, final String name, final String apkFile) {
        this.id = id;
        this.name = name;
        this.apkFile = apkFile;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getApkFile() {
        return apkFile;
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
        File f = new File(build.getWorkspace().getRemote() + "/" + apkFile);
        if (!f.exists()) {
            listener.getLogger().println(f.getAbsolutePath() + " does not exist! Aborting.");
            return false;
        }
        String server = getDescriptor().getServer();
        if (server == null || server.length() == 0) {
            listener.getLogger().println(
                    "NBU AppZone server not set. Please set in globa config! Aborting.");
        }

        HttpClient httpclient = new HttpClient();
        if (!server.endsWith("/")) {
            server += "/";
        }
        server += "app/" + id + "/android";
        PostMethod filePost = new PostMethod(server);
        try {
            if (name == null || name.length() == 0) {
                name = build.getProject().getName();
            }
            Part[] parts = {
                    new StringPart("version", getVersion(build, listener)),
                    new FilePart("apk", f),
            };
            filePost.setRequestEntity(new MultipartRequestEntity(parts, filePost.getParams()));
            listener.getLogger().println("[AppZone] Publishing to: " + server);
            httpclient.executeMethod(filePost);
            int statusCode = filePost.getStatusCode();
            if (statusCode < 200 || statusCode > 299) {
                String body = filePost.getResponseBodyAsString();
                listener.getLogger().println("[AppZone] Response (" + statusCode + "):" + body);
            }
            return true;
        } catch (IOException e) {
            listener.getLogger().print(e.getMessage());
        }
        return false;
    }

    private String getVersion(final AbstractBuild build, final BuildListener listener) {
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
                listener.getLogger().println("Error: " + e.getMessage());
            }
        } else {
            try {
                ZipFile zip = new ZipFile(build.getWorkspace().getRemote() + "/" + apkFile);
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
                listener.getLogger().println("Error: " + e.getMessage());
            }
        }
        return "NOT SET";
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
}
