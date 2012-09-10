package nl.liacs.subdisc.knime;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "SubgroupFilterNodeModel" Node.
 * Subgroup Filter for Cortana results.
 *
 * @author Marvin Meeng
 */
public class SubgroupFilterNodeModelNodeFactory 
        extends NodeFactory<SubgroupFilterNodeModelNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public SubgroupFilterNodeModelNodeModel createNodeModel() {
        return new SubgroupFilterNodeModelNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<SubgroupFilterNodeModelNodeModel> createNodeView(final int viewIndex,
            final SubgroupFilterNodeModelNodeModel nodeModel) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return null;
    }
}

