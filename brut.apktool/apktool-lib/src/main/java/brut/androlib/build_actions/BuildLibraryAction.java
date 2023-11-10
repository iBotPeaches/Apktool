package brut.androlib.build_actions;

import brut.androlib.build_actions.BuildAction;

public final class BuildLibraryAction extends BuildAction {
    private final String mFolder;

    public BuildLibraryAction(String folder) {
        mFolder = folder;
    }

    public String getFolder() {
        return mFolder;
    }
}
