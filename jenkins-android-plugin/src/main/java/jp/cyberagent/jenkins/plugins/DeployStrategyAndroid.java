
package jp.cyberagent.jenkins.plugins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

class DeployStrategyAndroid extends DeployStrategy {

    private final File mApkFile;
    private final Part[] mParts;

    public DeployStrategyAndroid(final String server, final String id, final File apkFile,
            final AbstractBuild build, final BuildListener listener) throws FileNotFoundException {
        super(server, "android", id, build, listener);
        mApkFile = apkFile;
        mParts = new Part[] {
                new StringPart("version", getVersion()),
                new FilePart("apk", apkFile),
        };
    }

    @Override
    public Part[] getParts() {
        return mParts;
    }

    @Override
    public String getVersion() {
        try {
            ZipFile zip = new ZipFile(mApkFile);
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
            getLogger().println(TAG + "Error: " + e.getMessage());
        }
        return super.getVersion();
    }
}
