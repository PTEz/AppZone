
package jp.cyberagent.jenkins.plugins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Collection;

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
    private final Part[] mParts;

    public DeployStrategyIOs(final String server, final String id, final File ipaFile,
            final AbstractBuild build, final BuildListener listener) throws FileNotFoundException {
        super(server, "ios", id, build, listener);
        mIpaFile = ipaFile;

        createManifest();
        mParts = new Part[] {
                new StringPart("version", getVersion()),
                new FilePart("ipa", ipaFile),
                new FilePart("manifest", getManifestFile()),
        };
    }

    @Override
    public Part[] getParts() {
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
        Collection<File> plistFiles = FileUtils.listFiles(
                new File(getBuild().getWorkspace().getRemote()),
                new AbstractFileFilter() {

                    @Override
                    public boolean accept(final File file) {
                        return file.getAbsolutePath().endsWith(".app/Info.plist");
                    }
                },
                FileFilterUtils.makeCVSAware(DirectoryFileFilter.DIRECTORY)
                );
        if (plistFiles.isEmpty()) {
            return false;
        } else if (plistFiles.size() > 1) {
            getLogger().println(TAG + "Error: Found multiple Info.plist files in *.app folders.");
            return false;
        }

        File manifestFile = getManifestFile();
        getLogger().println(TAG + "Creating manifest: " + manifestFile.getName());
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

            manifest = manifest.replace("${url}", getUrl() + ".ipa");
            manifest = manifest.replace("${CFBundleName}", bundleName);
            manifest = manifest.replace("${CFBundleIdentifier}", bundleIdentifier);
            manifest = manifest.replace("${CFBundleShortVersionString}",
                    bundleShortVersion);
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
}
