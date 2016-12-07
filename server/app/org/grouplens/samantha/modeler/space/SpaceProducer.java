package org.grouplens.samantha.modeler.space;

import javax.inject.Inject;

public class SpaceProducer {
    @Inject
    private IndexSpace indexSpace;
    @Inject
    private VariableSpace variableSpace;

    @Inject
    private SpaceProducer() {
    }

    public SpaceProducer(IndexSpace indexSpace, VariableSpace variableSpace) {
        this.indexSpace = indexSpace;
        this.variableSpace = variableSpace;
    }

    public IndexSpace getIndexSpace(String spaceName, SpaceMode spaceMode) {
        indexSpace.setSpaceState(spaceName, spaceMode);
        return indexSpace;
    }

    public VariableSpace getVariableSpace(String spaceName, SpaceMode spaceMode) {
        variableSpace.setSpaceState(spaceName, spaceMode);
        return variableSpace;
    }
}
