
package jp.cyberagent.jenkins.plugins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.AbstractFileFilter;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.PropertyListParser;

class DeployStrategyIOs extends DeployStrategy {

    private final File mIpaFile;
    private final File mPlistFile;
    private Part[] mParts;

    public DeployStrategyIOs(final String server, final String id, final String tag,
            final boolean prependNameToTag, final File ipaFile, final AbstractBuild build,
            final BuildListener listener) {
        super(server, "ios", id, tag, prependNameToTag, build, listener);
        mIpaFile = ipaFile;
        mPlistFile = findPlistFile();
        createManifest();
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
    public Part[] getParts() {
        if (mParts == null) {
            try {
                mParts = new Part[] {
                        new StringPart("version", getVersion()),
                        new FilePart("ipa", mIpaFile),
                        new FilePart("manifest", getManifestFile()),
                };
            } catch (FileNotFoundException e) {
                getLogger().println(TAG + "Error: " + e.getMessage());
            }
        }
        return mParts;
    }

    @Override
    public String getVersion() {
        try {
            File file = getManifestFile();
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(file);
            NSArray items = (NSArray) rootDict.objectForKey("items");
            NSDictionary item = (NSDictionary) items.objectAtIndex(0);
            NSDictionary metadata = (NSDictionary) item.objectForKey("metadata");
            return metadata.objectForKey("bundle-version").toString();
        } catch (Exception e) {
            getLogger().println(TAG + "Error: " + e.getMessage());
        }
        return super.getVersion();
    }

    private boolean createManifest() {
        File manifestFile = getManifestFile();
        getLogger().println(TAG + "Creating manifest: " + manifestFile.getName());
        try {
            if (mPlistFile == null) {
                getLogger().println(TAG + "No Info.plist file found. Aborting.");
                return false;
            }
            NSDictionary rootDict = (NSDictionary) PropertyListParser.parse(mPlistFile);
            String bundleName = rootDict.objectForKey("CFBundleName")
                    .toString();
            String bundleIdentifier = rootDict.objectForKey("CFBundleIdentifier")
                    .toString();
            String bundleShortVersionString = rootDict.objectForKey("CFBundleShortVersionString")
                    .toString();
            String bundleVersion = rootDict.objectForKey("CFBundleVersion").toString();

            InputStream inputStream = getClass().getResourceAsStream(
                    "/OTAManifestTemplate.plist");
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String manifest = writer.toString();

            manifest = manifest.replace("${url}", getUrl() + ".ipa");
            manifest = manifest.replace("${CFBundleName}", bundleName);
            manifest = manifest.replace("${CFBundleIdentifier}", bundleIdentifier);
            manifest = manifest.replace("${CFBundleShortVersionString}",
                    bundleShortVersionString + " (" + bundleVersion + ")");
            FileUtils.writeStringToFile(manifestFile, manifest);
        } catch (Exception e) {
            getLogger().println(
                    TAG + "Problmen creating manifest based on Info.plist: " + e.getMessage());
            return false;
        }
        return true;
    }

    private File getManifestFile() {
        String path = mIpaFile.getAbsolutePath();
        return new File(path.substring(0, path.length() - 4) + ".manifest");
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
