
package jp.cyberagent.jenkins.plugins;

import hudson.model.BuildListener;
import hudson.model.AbstractBuild;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;

abstract class DeployStrategy {
    public static final String TAG = "[AppZone] ";
    private final String mServer;
    private final String mType;
    private final String mId;
    private final String mTag;
    private final boolean mPrependNameToTag;
    private final AbstractBuild mBuild;
    private final BuildListener mListener;

    private String mUrl;
    private String mChangeLog;

    public DeployStrategy(final String server, final String type, final String id,
            final String tag, final boolean prependNameToTag, final AbstractBuild build,
            final BuildListener listener) {
        mServer = server;
        mType = type;
        mId = id;
        mTag = tag;
        mListener = listener;
        mPrependNameToTag = prependNameToTag;
        mBuild = build;
    }

    public void setChangeLog(final String changeLog) {
        mChangeLog = changeLog;
    }

    public String getUrl() {
        if (mUrl == null) {
            mUrl = createAppUrl(mServer, mType, mId, mTag);
        }
        return mUrl;
    }

    protected AbstractBuild getBuild() {
        return mBuild;
    }

    protected BuildListener getListener() {
        return mListener;
    }

    protected PrintStream getLogger() {
        return mListener.getLogger();
    }

    public String getVersion() {
        File versionFile = new File(mBuild.getWorkspace().getRemote() + "/VERSION");
        if (versionFile.exists()) {
            try {
                FileInputStream fstream = new FileInputStream(versionFile);
                DataInputStream in = new DataInputStream(fstream);
                BufferedReader br = new BufferedReader(new InputStreamReader(in));
                String firstLine = br.readLine();
                in.close();
                return firstLine;
            } catch (Exception e) {
                getLogger().println(TAG + "Error: " + e.getMessage());
            }
        }
        return null;
    }

    private String createAppUrl(final String server, final String type, final String id,
            String tag) {
        StringBuilder url = new StringBuilder(server);
        if (!server.endsWith("/")) {
            url.append("/");
        }
        url.append("app/" + id + "/" + type);

        tag = tag != null ? tag.trim() : "";

        if (mPrependNameToTag || !tag.isEmpty()) {
            url.append("/");

            if (mPrependNameToTag) {
                url.append(getDeployableName());
                if (!tag.isEmpty()) {
                    url.append("-");
                }
            }
            url.append(tag);
        }
        return url.toString();
    }

    public List<Part> getParts() throws FileNotFoundException {
        List<Part> list = new LinkedList<Part>();
        list.add(new StringPart("version", getVersion()));
        if (mChangeLog != null && mChangeLog.length() > 0) {
            list.add(new StringPart("changelog", mChangeLog, "UTF-8"));
        }
        return list;
    }

    public abstract String getDeployableName();
}
