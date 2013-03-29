
package jp.co.cyberagent.jenkins.plugins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.Part;

import brut.androlib.AndrolibException;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.value.ResStringValue;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.util.ExtFile;

class DeployStrategyAndroid extends DeployStrategy {

    private final File mApkFile;

    public DeployStrategyAndroid(final String server, final String id, final String tag,
            final boolean prependNameToTag, final File apkFile, final AbstractBuild build,
            final BuildListener listener) {
        super(server, "android", id, tag, prependNameToTag, build, listener);
        mApkFile = apkFile;
    }

    @Override
    public List<Part> getParts() throws FileNotFoundException {
        List<Part> parts = super.getParts();
        parts.add(new FilePart("apk", mApkFile));
        return parts;
    }

    @Override
    public String getVersion() {
        String version = getStringFromManifest("versionName");
        if (version != null) {
            return version;
        }
        return super.getVersion();
    }

    @Override
    public String getDeployableName() {
        return getStringFromManifest("android:label");
    }

    private String getStringFromManifest(final String name) {
        try {
            ZipFile zip = new ZipFile(mApkFile);
            ZipEntry mft = zip.getEntry("AndroidManifest.xml");
            InputStream is = zip.getInputStream(mft);

            byte[] xml = new byte[is.available()];
            is.read(xml);

            String string = AndroidUtils.decompressXML(xml);
            int start = string.indexOf(name + "=\"") + name.length() + 2;
            int end = string.indexOf("\"", start);
            String version = string.substring(start, end);

            if (version.startsWith("resourceID 0x")) {
                int resId = Integer.parseInt(version.substring(13), 16);
                return getStringFromResource(resId);
            } else {
                return version;
            }
        } catch (Exception e) {
            getLogger().println(TAG + "Error: " + e.getMessage());
        }
        return null;
    }

    private String getStringFromResource(final int resId) throws AndrolibException {
        AndrolibResources res = new AndrolibResources();
        ExtFile file = new ExtFile(mApkFile);
        ResTable table = res.getResTable(file);
        ResPackage defaultPackage = table.listMainPackages().iterator().next();

        ResValue value = table.getValue(defaultPackage.getName(),
                "string", table.getResSpec(resId).getName());
        return ((ResStringValue) value).encodeAsResXmlValue();
    }
}
