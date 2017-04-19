package ru.runa.wfe.graph.history;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import ru.runa.wfe.graph.DrawProperties;
import ru.runa.wfe.graph.RenderHits;
import ru.runa.wfe.graph.image.figure.AbstractFigure;
import ru.runa.wfe.graph.image.figure.TransitionFigure;
import ru.runa.wfe.history.graph.HistoryGraphForkNodeModel;
import ru.runa.wfe.history.graph.HistoryGraphGenericNodeModel;
import ru.runa.wfe.history.graph.HistoryGraphJoinNodeModel;
import ru.runa.wfe.history.graph.HistoryGraphNode;
import ru.runa.wfe.history.graph.HistoryGraphNodeVisitor;
import ru.runa.wfe.history.graph.HistoryGraphParallelNodeModel;
import ru.runa.wfe.history.graph.HistoryGraphTransitionModel;

/**
 * Creates image from history graph.
 */
public class CreateHistoryGraphImage implements HistoryGraphNodeVisitor<CreateHistoryGraphImageContext> {

    private static final String FORMAT = "png";
    private final BufferedImage resultImage;
    private final Graphics2D graphics;

    public CreateHistoryGraphImage(int maxHeight, int maxWidth) {
        super();
        resultImage = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_RGB);
        graphics = resultImage.createGraphics();
        graphics.setFont(new Font(DrawProperties.getFontFamily(), Font.PLAIN, DrawProperties.getFontSize()));
        graphics.setColor(DrawProperties.getBackgroundColor());
        graphics.fillRect(0, 0, maxWidth, maxHeight);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
    }

    /**
     * Get image, generated by process history graph.
     */
    public byte[] getImageBytes() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(resultImage, FORMAT, outputStream);
        return outputStream.toByteArray();
    }

    @Override
    public void onForkNode(HistoryGraphForkNodeModel node, CreateHistoryGraphImageContext context) {
        commonProcessNode(node, context);
    }

    @Override
    public void onJoinNode(HistoryGraphJoinNodeModel node, CreateHistoryGraphImageContext context) {
        commonProcessNode(node, context);
    }

    @Override
    public void onParallelNode(HistoryGraphParallelNodeModel node, CreateHistoryGraphImageContext context) {
        commonProcessNode(node, context);
    }

    @Override
    public void onGenericNode(HistoryGraphGenericNodeModel node, CreateHistoryGraphImageContext context) {
        commonProcessNode(node, context);
    }

    /**
     * Common node processing logic. All drawing is the same for all nodes.
     * 
     * @param node
     *            Node to drawing.
     * @param context
     *            Operation context
     */
    private void commonProcessNode(HistoryGraphNode node, CreateHistoryGraphImageContext context) {
        FiguresNodeData data = FiguresNodeData.getOrThrow(node);
        for (HistoryGraphTransitionModel transition : node.getTransitions()) {
            transition.getToNode().processBy(this, context);
        }
        drawTransitions(data);
        drawNode(data);
    }

    /**
     * Draw transition.
     * 
     * @param data
     *            Data with figures to draw.
     */
    private void drawTransitions(FiguresNodeData data) {
        for (TransitionFigure transition : data.getTransitions()) {
            transition.draw(graphics, transition.getRenderHits().getColor());
        }
    }

    /**
     * Draw node.
     * 
     * @param data
     *            Data with figures to draw.
     */
    private void drawNode(FiguresNodeData data) {
        RenderHits hits = data.getFigure().getRenderHits();
        int lineWidth = 2;
        AbstractFigure figure = data.getFigure();
        graphics.setStroke(new BasicStroke(lineWidth));
        graphics.setColor(hits.getColor());
        figure.draw(graphics, true);
    }
}
