
package jp.co.cyberagent.jenkins.plugins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

class DeployStrategyIOs extends DeployStrategy {

    private final File mIpaFile;
    private final File mPlistFile;

    public DeployStrategyIOs(final String server, final String id, final String tag,
            final boolean prependNameToTag, final File ipaFile, final AbstractBuild build,
            final BuildListener listener) {
        super(server, "ios", id, tag, prependNameToTag, build, listener);
        mIpaFile = ipaFile;
        mPlistFile = findPlistFile();
    }

    private File findPlistFile() {
        Collection<File> plistFiles = FileUtils.listFiles(
                new File(getBuild().getWorkspace().getRemote()),
                new AbstractFileFilter() {

                    @Override
                    public boolean accept(final File file) {
                        if (file.getAbsolutePath().endsWith(".app/Info.plist")) {
                            Pattern pattern = Pattern
                                    .compile(".*\\/([^\\.\\/]+)\\.app\\/Info.plist");
                            Matcher matcher = pattern.matcher(file.getAbsolutePath());
                            return matcher.matches()
                                    && mIpaFile.getName().startsWith(matcher.group(1));
                        } else {
                            return false;
                        }
                    }
                },
                FileFilterUtils.makeCVSAware(DirectoryFileFilter.DIRECTORY)
                );
        if (plistFiles.isEmpty()) {
            return null;
        } else if (plistFiles.size() > 1) {
            getLogger().println(TAG + "Error: Found multiple Info.plist files in *.app folders.");
            return null;
        }
        return plistFiles.iterator().next();
    }

    @Override
    public List<Part> getParts() throws FileNotFoundException {
        List<Part> parts = super.getParts();
        parts.add(new FilePart("ipa", mIpaFile));
        return parts;
    }

    @Override
    public String getVersion() {
        try {
            return getBundleVersionVersion();
        } catch (Exception e) {
            getLogger().println(TAG + "Error: " + e.getMessage());
        }
        return super.getVersion();
    }

    private String getBundleVersionVersion() throws Exception {
        if (mPlistFile == null) {
            getLogger().println(TAG + "No Info.plist file found. Aborting.");
            return null;
        }
        NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(mPlistFile);

        String bundleShortVersionString = rootDict.objectForKey("CFBundleShortVersionString")
                .toString();
        String bundleVersion = rootDict.objectForKey("CFBundleVersion").toString();
        return bundleShortVersionString + " (" + bundleVersion + ")";
    }

    @Override
    public String getDeployableName() {
        if (mPlistFile == null) {
            getLogger().println(TAG + "No Info.plist file found. Aborting.");
            return null;
        }
        try {
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(mPlistFile);
            return rootDict.objectForKey("CFBundleDisplayName").toString();
        } catch (Exception e) {
            return null;
        }
    }
}
