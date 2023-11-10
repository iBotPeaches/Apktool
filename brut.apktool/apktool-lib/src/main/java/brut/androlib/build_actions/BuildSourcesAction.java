package brut.androlib.build_actions;

public final class BuildSourcesAction extends BuildAction {
    private final String mFolder;
    private final String mFileName;
    private final boolean mBuildSmali;

    public BuildSourcesAction(String folder, String fileName, boolean buildSmali) {
        mFolder = folder;
        mFileName = fileName;
        mBuildSmali = buildSmali;
    }

    public String getFolder() {
        return mFolder;
    }

    public String getFileName() {
        return mFileName;
    }

    public boolean getBuildSmali() {
        return mBuildSmali;
    }
}
