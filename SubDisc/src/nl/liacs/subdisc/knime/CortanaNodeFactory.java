package nl.liacs.subdisc.knime;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "Cortana" Node.
 * Subgroup Discovery using Cortana.
 *
 * @author Marvin Meeng
 */
public class CortanaNodeFactory 
        extends NodeFactory<CortanaNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public CortanaNodeModel createNodeModel() {
        return new CortanaNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<CortanaNodeModel> createNodeView(final int viewIndex,
            final CortanaNodeModel nodeModel) {
        return new CortanaNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new CortanaNodeDialog();
    }

}

