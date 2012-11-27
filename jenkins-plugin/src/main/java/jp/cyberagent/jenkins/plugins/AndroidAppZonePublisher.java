
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

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;

import net.sf.json.JSONObject;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

public class AndroidAppZonePublisher extends Notifier {
    public static final String DEFAULT_APPSERVER = "http://appzone-api.pes.ch/";

    private static final String TAG = "[AppZone] ";

    private final String id;
    private final String tag;

    private final boolean prependNameToTag;

    // Fields in config.jelly must match the parameter names in the
    // "DataBoundConstructor"
    @DataBoundConstructor
    public AndroidAppZonePublisher(final String id, final String tag, final boolean prependNameToTag) {
        this.id = id;
        this.tag = tag;
        this.prependNameToTag = prependNameToTag;
    }

    public String getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }

    public boolean getPrependNameToTag() {
        return prependNameToTag;
    }

    public FormValidation doCheckId(@QueryParameter
    final String value)
            throws IOException, ServletException {
        if (value.length() == 0) {
            return FormValidation.error("ID needs to be set.");
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

        Collection<File> files = getPossibleAppFiles(build, listener);
        if (files.isEmpty()) {
            listener.getLogger().println(TAG + "No file to puslish found. Skip.");
            return true;
        }
        Iterator<File> fileIterator = files.iterator();
        while (fileIterator.hasNext()) {
            try {
                File file = fileIterator.next();
                String fileName = file.getName();
                DeployStrategy deploy;
                listener.getLogger().println(TAG + "File: " + fileName);
                if (fileName.endsWith(".apk")) {
                    deploy = new DeployStrategyAndroid(server, id, tag, prependNameToTag, file,
                            build,
                            listener);
                } else if (fileName.endsWith(".ipa")) {
                    deploy = new DeployStrategyIOs(server, id, tag, prependNameToTag, file, build,
                            listener);
                } else {
                    return false;
                }
                listener.getLogger().println(TAG + "Version: " + deploy.getVersion());
                listener.getLogger().println(TAG + "Publishing to: " + deploy.getUrl());

                HttpClient httpclient = new HttpClient();
                PostMethod filePost = new PostMethod(deploy.getUrl());
                filePost.setRequestEntity(
                        new MultipartRequestEntity(deploy.getParts(), filePost.getParams()));
                httpclient.executeMethod(filePost);
                int statusCode = filePost.getStatusCode();
                if (statusCode < 200 || statusCode > 299) {
                    String body = filePost.getResponseBodyAsString();
                    listener.getLogger().println(TAG + "Response (" + statusCode + "):" + body);
                    return false;
                }
            } catch (IOException e) {
                listener.getLogger().print(e.getMessage());
                return false;
            }
        }
        return true;
    }

    private Collection<File> getPossibleAppFiles(final AbstractBuild build,
            final BuildListener listener) {
        File dir = new File(build.getWorkspace().getRemote());
        Collection<File> files = FileUtils.listFiles(
                dir,
                new RegexFileFilter("(.(?!unaligned)(?!unsigned))*(\\.apk|\\.ipa)"),
                FileFilterUtils.makeCVSAware(DirectoryFileFilter.DIRECTORY)
                );
        List<File> removeFiles = new LinkedList<File>();
        for (File file : files) {
            if (file.getAbsolutePath().endsWith(".apk")) {
                File propertiesFile = new File(file.getParentFile().getParentFile(),
                        "project.properties");
                try {
                    boolean isLibrary = FileUtils.readFileToString(propertiesFile)
                            .contains("android.library=true");
                    if (isLibrary) {
                        removeFiles.add(file);
                    }
                } catch (Exception e) {
                    removeFiles.add(file);
                }
            }
        }
        files.removeAll(removeFiles);
        return files;
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
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
