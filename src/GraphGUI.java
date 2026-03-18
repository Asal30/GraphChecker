import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;

// ============================================================
//  GraphGUI.java  —  Visual GUI for the Sink Elimination Algorithm
//
//  HOW IT WORKS:
//    1. The window is split into two halves:
//       LEFT  = the canvas where the graph is drawn
//       RIGHT = control panel (buttons, log, adjacency list)
//
//    2. The user can:
//       - Click the canvas to add vertices
//       - Click two vertices in sequence to add an edge
//       - Load a preset graph (acyclic or cyclic)
//       - Run the algorithm and step through it
//
//    3. The algorithm logic is the same sink-elimination
//       you already know — this file just visualises it.
// ============================================================

public class GraphGUI extends JFrame {

    // ── Inner class: represents one vertex on screen ──────────────────────
    static class VertexNode {
        int id;            // unique number (0, 1, 2, ...)
        String label;      // display label ("A", "B", "C", ...)
        int x, y;          // position on the canvas (pixels)
        String state;      // "normal" | "sink" | "removed" | "cycle"

        VertexNode(int id, String label, int x, int y) {
            this.id    = id;
            this.label = label;
            this.x     = x;
            this.y     = y;
            this.state = "normal";
        }
    }

    // ── Inner class: represents one directed edge ──────────────────────────
    static class EdgeLine {
        int fromId, toId;  // the two vertex IDs this edge connects

        EdgeLine(int fromId, int toId) {
            this.fromId = fromId;
            this.toId   = toId;
        }
    }

    // ── Inner class: one snapshot of the algorithm at a single step ────────
    static class AlgoStep {
        Set<Integer> removed;   // which vertex IDs have been removed so far
        int          sinkId;    // the vertex being removed at THIS step (-1 if none)
        Set<Integer> cycleIds;  // vertices that form the detected cycle (empty if none)
        List<int[]>  cycleEdges;// edges that are part of the cycle
        String       description; // human-readable explanation for this step
        String       verdict;   // "yes", "no", or null (not final yet)

        AlgoStep(Set<Integer> removed, int sinkId, String description) {
            this.removed     = new HashSet<>(removed);
            this.sinkId      = sinkId;
            this.cycleIds    = new HashSet<>();
            this.cycleEdges  = new ArrayList<>();
            this.description = description;
            this.verdict     = null;
        }
    }

    // ── Constants ──────────────────────────────────────────────────────────
    static final int    RADIUS   = 22;   // radius of each vertex circle (pixels)
    static final Color  C_NORMAL = new Color(0xE6F1FB); // light blue fill
    static final Color  C_SINK   = new Color(0xEAF3DE); // light green fill  (sink)
    static final Color  C_REMOVE = new Color(0xF1EFE8); // light grey fill   (removed)
    static final Color  C_CYCLE  = new Color(0xFCEBEB); // light red fill    (cycle)
    static final Color  B_NORMAL = new Color(0x185FA5); // blue border
    static final Color  B_SINK   = new Color(0x3B6D11); // green border
    static final Color  B_REMOVE = new Color(0xB4B2A9); // grey border
    static final Color  B_CYCLE  = new Color(0xA32D2D); // red border
    static final Color  T_NORMAL = new Color(0x0C447C); // blue text
    static final Color  T_SINK   = new Color(0x27500A); // green text
    static final Color  T_REMOVE = new Color(0x888780); // grey text
    static final Color  T_CYCLE  = new Color(0x791F1F); // red text

    // ── Graph data ─────────────────────────────────────────────────────────
    List<VertexNode> vertices   = new ArrayList<>();
    List<EdgeLine>   edges      = new ArrayList<>();
    int              nextId     = 0;  // auto-increment ID for new vertices

    // ── Algorithm state ────────────────────────────────────────────────────
    List<AlgoStep>   steps      = new ArrayList<>();
    int              stepIndex  = -1; // which step we're currently showing (-1 = not started)
    boolean          algoActive = false;

    // ── Interaction state ──────────────────────────────────────────────────
    String           mode       = "vertex"; // "vertex" | "edge" | "move" | "delete"
    VertexNode       edgeSource = null;     // first vertex clicked in edge-drawing mode
    VertexNode       dragging   = null;     // vertex being dragged in move mode
    int              dragOffX, dragOffY;    // offset between mouse and vertex centre

    // ── Swing components ───────────────────────────────────────────────────
    GraphCanvas      canvas;
    JTextArea        logArea;
    JTextArea        adjArea;
    JLabel           descLabel;
    JLabel           verdictLabel;
    JButton          btnRun, btnPrev, btnNext, btnReset;
    JLabel           stepCounter;

    // ══════════════════════════════════════════════════════════════════════
    //  Constructor — builds the entire window layout
    // ══════════════════════════════════════════════════════════════════════
    public GraphGUI() {
        super("Graph Acyclicity Checker — Sink Elimination");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        getContentPane().setBackground(Color.WHITE);

        // ── Top toolbar ───────────────────────────────────────────────────
        JPanel toolbar = buildToolbar();
        add(toolbar, BorderLayout.NORTH);

        // ── Centre: canvas (left) + controls (right) ──────────────────────
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(520);
        split.setResizeWeight(0.65);
        split.setBorder(null);

        canvas = new GraphCanvas();
        split.setLeftComponent(canvas);
        split.setRightComponent(buildRightPanel());
        add(split, BorderLayout.CENTER);

        // ── Start with the acyclic preset loaded ──────────────────────────
        loadPreset("acyclic");

        // ── Window size and display ───────────────────────────────────────
        setSize(960, 600);
        setLocationRelativeTo(null); // centre on screen
        setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Build the top toolbar with mode buttons and preset loaders
    // ══════════════════════════════════════════════════════════════════════
    JPanel buildToolbar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6));
        p.setBackground(new Color(0xF8F8F8));
        p.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xDDDDDD)));

        // Mode buttons
        p.add(makeLabel("Mode:"));
        p.add(modeBtn("+ Vertex", "vertex"));
        p.add(modeBtn("→ Edge",   "edge"));
        p.add(modeBtn("Move",     "move"));
        p.add(modeBtn("Delete",   "delete"));

        p.add(new JSeparator(JSeparator.VERTICAL));
        p.add(makeLabel("Preset:"));

        // Preset buttons
        JButton ba = styledBtn("Acyclic example");
        ba.addActionListener(e -> { resetAlgo(); loadPreset("acyclic"); });
        p.add(ba);

        JButton bc = styledBtn("Cyclic example");
        bc.addActionListener(e -> { resetAlgo(); loadPreset("cyclic"); });
        p.add(bc);

        JButton bx = styledBtn("Clear all");
        bx.setForeground(new Color(0xA32D2D));
        bx.addActionListener(e -> { resetAlgo(); vertices.clear(); edges.clear(); nextId = 0; refresh(); });
        p.add(bx);

        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Build the right-side control panel
    // ══════════════════════════════════════════════════════════════════════
    JPanel buildRightPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBorder(new EmptyBorder(8, 8, 8, 8));
        p.setBackground(Color.WHITE);

        // Step description box
        descLabel = new JLabel("<html>Load a graph then click <b>Run algorithm</b>.</html>");
        descLabel.setFont(new Font("SansSerif", Font.PLAIN, 13));
        descLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xDDDDDD), 1),
                new EmptyBorder(8, 10, 8, 10)));
        descLabel.setOpaque(true);
        descLabel.setBackground(new Color(0xF5F8FF));
        descLabel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        descLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(descLabel);
        p.add(Box.createVerticalStrut(8));

        // Verdict badge
        verdictLabel = new JLabel(" ");
        verdictLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        verdictLabel.setOpaque(true);
        verdictLabel.setBackground(Color.WHITE);
        verdictLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        verdictLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(verdictLabel);
        p.add(Box.createVerticalStrut(8));

        // Navigation buttons
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        nav.setBackground(Color.WHITE);
        nav.setAlignmentX(Component.LEFT_ALIGNMENT);

        btnRun   = styledBtn("Run algorithm");
        btnPrev  = styledBtn("← Back");
        btnNext  = styledBtn("Next →");
        btnReset = styledBtn("Reset");
        stepCounter = new JLabel("");
        stepCounter.setFont(new Font("SansSerif", Font.PLAIN, 12));
        stepCounter.setForeground(Color.GRAY);

        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);
        btnReset.setVisible(false);

        btnRun.addActionListener(e   -> runAlgorithm());
        btnPrev.addActionListener(e  -> moveStep(-1));
        btnNext.addActionListener(e  -> moveStep(+1));
        btnReset.addActionListener(e -> resetAlgo());

        nav.add(btnRun); nav.add(btnPrev); nav.add(btnNext);
        nav.add(btnReset); nav.add(stepCounter);
        p.add(nav);
        p.add(Box.createVerticalStrut(10));

        // Adjacency list display
        p.add(sectionLabel("Adjacency list"));
        adjArea = new JTextArea(6, 20);
        adjArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        adjArea.setEditable(false);
        adjArea.setBackground(new Color(0xF8F8F8));
        adjArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane adjScroll = new JScrollPane(adjArea);
        adjScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        adjScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));
        p.add(adjScroll);
        p.add(Box.createVerticalStrut(10));

        // Algorithm log
        p.add(sectionLabel("Algorithm log"));
        logArea = new JTextArea(8, 20);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setEditable(false);
        logArea.setBackground(new Color(0xF8F8F8));
        logArea.setBorder(new EmptyBorder(6, 8, 6, 8));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        logScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        p.add(logScroll);

        return p;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  GraphCanvas — the inner class that draws the graph
    //
    //  paintComponent() is called every time we call repaint().
    //  It draws: edges (arrows), then vertices (circles with labels).
    // ══════════════════════════════════════════════════════════════════════
    class GraphCanvas extends JPanel {

        GraphCanvas() {
            setBackground(Color.WHITE);
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));

            // ── Mouse click handler ───────────────────────────────────────
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (algoActive) return; // don't allow editing while algorithm runs

                    Point p = e.getPoint();

                    switch (mode) {
                        case "vertex":
                            // Only add a vertex if click is not on an existing one
                            if (vertexAt(p) == null) {
                                String lbl = labelFor(nextId);
                                vertices.add(new VertexNode(nextId++, lbl, p.x, p.y));
                                refresh();
                            }
                            break;

                        case "edge":
                            VertexNode clicked = vertexAt(p);
                            if (clicked != null) {
                                if (edgeSource == null) {
                                    // First click — remember source vertex
                                    edgeSource = clicked;
                                } else if (edgeSource.id != clicked.id) {
                                    // Second click — add the edge (if it doesn't exist)
                                    boolean exists = edges.stream()
                                            .anyMatch(ed -> ed.fromId == edgeSource.id && ed.toId == clicked.id);
                                    if (!exists) {
                                        edges.add(new EdgeLine(edgeSource.id, clicked.id));
                                    }
                                    edgeSource = null;
                                    refresh();
                                } else {
                                    edgeSource = null; // clicked same vertex twice — cancel
                                }
                                repaint();
                            }
                            break;

                        case "move":
                            VertexNode v = vertexAt(p);
                            if (v != null) {
                                dragging  = v;
                                dragOffX  = p.x - v.x;
                                dragOffY  = p.y - v.y;
                            }
                            break;

                        case "delete":
                            VertexNode del = vertexAt(p);
                            if (del != null) {
                                vertices.removeIf(vv -> vv.id == del.id);
                                edges.removeIf(ed -> ed.fromId == del.id || ed.toId == del.id);
                            } else {
                                // Try to delete an edge if user clicks near one
                                EdgeLine near = edgeNear(p);
                                if (near != null) edges.remove(near);
                            }
                            refresh();
                            break;
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    dragging = null;
                }
            });

            // ── Mouse drag handler (for "move" mode) ─────────────────────
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (dragging != null) {
                        dragging.x = e.getX() - dragOffX;
                        dragging.y = e.getY() - dragOffY;
                        repaint();
                    }
                }
            });
        }

        // ── paintComponent: called automatically whenever repaint() is called ──
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            // Enable anti-aliasing (makes curves and text look smooth)
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            // Get current algorithm state (which nodes removed, which is sink, cycle)
            Set<Integer> removed   = getCurrentRemoved();
            int          currentSink = getCurrentSink();
            Set<Integer> cycleIds  = getCurrentCycleIds();
            Set<String>  cycleEdgeKeys = getCurrentCycleEdgeKeys();

            // ── Draw edges first (so vertices appear on top) ──────────────
            for (EdgeLine ed : edges) {
                VertexNode src = findVertex(ed.fromId);
                VertexNode tgt = findVertex(ed.toId);
                if (src == null || tgt == null) continue;

                boolean isRemovedEdge = removed.contains(ed.fromId) || removed.contains(ed.toId);
                boolean isCycleEdge  = cycleEdgeKeys.contains(ed.fromId + "-" + ed.toId);

                Color edgeColor;
                if      (isCycleEdge)   edgeColor = new Color(0xE24B4A); // red for cycle edges
                else if (isRemovedEdge) edgeColor = new Color(0xCCCCCC); // grey for removed
                else                    edgeColor = new Color(0x378ADD); // blue for normal

                drawArrow(g2, src.x, src.y, tgt.x, tgt.y, edgeColor, isRemovedEdge);
            }

            // ── Draw dashed preview line when adding an edge ──────────────
            if (edgeSource != null && mode.equals("edge")) {
                g2.setColor(new Color(0xEF9F27));
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{5f, 4f}, 0f));
                g2.drawOval(edgeSource.x - RADIUS - 4, edgeSource.y - RADIUS - 4,
                        (RADIUS + 4) * 2, (RADIUS + 4) * 2);
                g2.setStroke(new BasicStroke(1.5f));
            }

            // ── Draw vertices ─────────────────────────────────────────────
            for (VertexNode v : vertices) {
                boolean isRemoved = removed.contains(v.id);
                boolean isSink    = (v.id == currentSink);
                boolean isCycle   = cycleIds.contains(v.id);

                Color fill, border, textColor;
                float borderWidth;

                if      (isRemoved) { fill = C_REMOVE; border = B_REMOVE; textColor = T_REMOVE; borderWidth = 0.5f; }
                else if (isCycle)   { fill = C_CYCLE;  border = B_CYCLE;  textColor = T_CYCLE;  borderWidth = 2f;   }
                else if (isSink)    { fill = C_SINK;   border = B_SINK;   textColor = T_SINK;   borderWidth = 2f;   }
                else                { fill = C_NORMAL; border = B_NORMAL; textColor = T_NORMAL; borderWidth = 0.5f; }

                // Draw filled circle
                g2.setColor(fill);
                g2.fillOval(v.x - RADIUS, v.y - RADIUS, RADIUS * 2, RADIUS * 2);

                // Draw border circle
                g2.setColor(border);
                g2.setStroke(new BasicStroke(borderWidth));
                g2.drawOval(v.x - RADIUS, v.y - RADIUS, RADIUS * 2, RADIUS * 2);

                // Draw label text
                g2.setColor(textColor);
                g2.setFont(new Font("SansSerif", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                int textW = fm.stringWidth(v.label);
                g2.drawString(v.label, v.x - textW / 2, v.y + fm.getAscent() / 2 - 1);

                // Draw strikethrough on removed vertices
                if (isRemoved) {
                    g2.setColor(B_REMOVE);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawLine(v.x - 10, v.y, v.x + 10, v.y);
                }

                g2.setStroke(new BasicStroke(1f));
            }
        }

        // ── Helper: draw an arrow from (x1,y1) to (x2,y2) ────────────────
        void drawArrow(Graphics2D g2, int x1, int y1, int x2, int y2, Color color, boolean dashed) {
            double dx  = x2 - x1, dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1) return;

            double nx = dx / len, ny = dy / len;

            // Shorten line so it starts/ends at circle edge
            double sx = x1 + nx * RADIUS;
            double sy = y1 + ny * RADIUS;
            double ex = x2 - nx * (RADIUS + 9);
            double ey = y2 - ny * (RADIUS + 9);

            g2.setColor(color);
            if (dashed) {
                g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                        1f, new float[]{5f, 4f}, 0f));
            } else {
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            }

            g2.draw(new Line2D.Double(sx, sy, ex, ey));
            g2.setStroke(new BasicStroke(1.5f));

            // Draw arrowhead (small filled triangle at the end)
            double angle = Math.atan2(ey - sy, ex - sx);
            int   al = 10; // arrowhead length and half-width
            int[] xPts = {
                    (int) ex,
                    (int) (ex - al * Math.cos(angle - 0.4)),
                    (int) (ex - al * Math.cos(angle + 0.4))
            };
            int[] yPts = {
                    (int) ey,
                    (int) (ey - al * Math.sin(angle - 0.4)),
                    (int) (ey - al * Math.sin(angle + 0.4))
            };
            g2.fillPolygon(xPts, yPts, 3);
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Algorithm logic — builds the list of steps
    // ══════════════════════════════════════════════════════════════════════
//    void runAlgorithm() {
//        if (vertices.isEmpty()) {
//            descLabel.setText("Add some vertices first!");
//            return;
//        }
//
//        steps.clear();
//        logArea.setText("");
//        Set<Integer> removedSoFar = new HashSet<>();
//
//        // Step 0: initial state
//        steps.add(new AlgoStep(removedSoFar, -1, "Starting. Looking for vertices with no outgoing edges (sinks)."));
//
//        for (int iter = 0; iter <= vertices.size() + 1; iter++) {
//
//            // Get currently alive vertices
//            List<VertexNode> alive = new ArrayList<>();
//            for (VertexNode v : vertices) {
//                if (!removedSoFar.contains(v.id)) alive.add(v);
//            }
//
//            // Check if graph is empty
//            if (alive.isEmpty()) {
//                AlgoStep s = new AlgoStep(removedSoFar, -1,
//                        "The graph is empty — every vertex was successfully removed as a sink. The graph is ACYCLIC.");
//                s.verdict = "yes";
//                steps.add(s);
//                break;
//            }
//
//            // Find a sink (vertex with no outgoing edges to alive vertices)
//            VertexNode sink = null;
//            for (VertexNode v : alive) {
//                Set<Integer> finalRemovedSoFar = removedSoFar;
//                boolean hasSomeOut = edges.stream()
//                        .anyMatch(ed -> ed.fromId == v.id && !finalRemovedSoFar.contains(ed.toId));
//                if (!hasSomeOut) { sink = v; break; }
//            }
//
//            if (sink == null) {
//                // No sink found — cycle exists
//                AlgoStep s = new AlgoStep(removedSoFar, -1, "");
//                Map<Integer, Integer> parentMap = new HashMap<>();
//                int[] cycleEnds = findCycleByDFS(alive, removedSoFar, parentMap);
//                if (cycleEnds != null) {
//                    List<Integer> cyclePath = reconstructCycle(cycleEnds[0], cycleEnds[1], parentMap);
//                    StringBuilder pathStr = new StringBuilder();
//                    for (int i = 0; i < cyclePath.size(); i++) {
//                        VertexNode vn = findVertex(cyclePath.get(i));
//                        pathStr.append(vn != null ? vn.label : "?");
//                        if (i < cyclePath.size() - 1) pathStr.append(" → ");
//                    }
//                    s.description = "No sink found — every remaining vertex has at least one outgoing edge. " +
//                            "A cycle exists: " + pathStr;
//                    s.cycleIds.addAll(cyclePath);
//                    for (int i = 0; i < cyclePath.size() - 1; i++)
//                        s.cycleEdges.add(new int[]{cyclePath.get(i), cyclePath.get(i+1)});
//                } else {
//                    s.description = "No sink found — a cycle exists.";
//                }
//                s.verdict = "no";
//                steps.add(s);
//                break;
//            }
//
//            // Remove the sink
//            removedSoFar = new HashSet<>(removedSoFar);
//            removedSoFar.add(sink.id);
//            steps.add(new AlgoStep(removedSoFar, sink.id,
//                    "Vertex \"" + sink.label + "\" has no outgoing edges → it is a SINK. Removing it."));
//        }
//
//        // Activate step navigation
//        algoActive = true;
//        stepIndex  = 0;
//        btnRun.setVisible(false);
//        btnReset.setVisible(true);
//        btnNext.setEnabled(true);
//        btnPrev.setEnabled(false);
//        renderStep();
//    }

    void runAlgorithm() {
        if (vertices.isEmpty()) {
            descLabel.setText("Add some vertices first!");
            return;
        }

        steps.clear();
        logArea.setText("");

        // ── Step 1: Build your real Graph object from the GUI vertices/edges ──
        // This is where GraphGUI connects to your Graph.java class.
        Graph g = new Graph();
        for (VertexNode v : vertices) {
            g.addVertex(v.id);
        }
        for (EdgeLine e : edges) {
            g.addEdge(e.fromId, e.toId);
        }

        // ── Step 2: Run the algorithm and record each step for the GUI ────────
        // We do NOT call CycleChecker.isAcyclic(g) directly here because that
        // method destroys the graph without saving steps for us to display.
        // Instead we re-implement the same logic here, but save a snapshot
        // (AlgoStep) at each point so the GUI can show them one by one.

        Set<Integer> removedSoFar = new HashSet<>();

        // Step 0: initial state — nothing removed yet
        steps.add(new AlgoStep(removedSoFar, -1,
                "Starting. Looking for vertices with no outgoing edges (sinks)."));

        for (int iter = 0; iter <= vertices.size() + 1; iter++) {

            // Ask your real Graph object which vertices are still alive
            // (Graph.getVertices() returns only non-removed vertices)
            // We mirror that by filtering our GUI vertex list the same way.
            List<VertexNode> alive = new ArrayList<>();
            for (VertexNode v : vertices) {
                if (!removedSoFar.contains(v.id)) alive.add(v);
            }

            // Rule 1: if the graph is empty, it is acyclic — we are done
            if (alive.isEmpty()) {
                AlgoStep s = new AlgoStep(removedSoFar, -1,
                        "The graph is empty — every vertex was removed as a sink. The graph is ACYCLIC.");
                s.verdict = "yes";
                steps.add(s);
                break;
            }

            // Rule 2: ask your real Graph object to find a sink
            // Graph.findSink() returns -1 if no sink exists
            // We rebuild a temporary Graph at this state so findSink() works correctly
            Graph current = new Graph();
            for (VertexNode v : alive)          current.addVertex(v.id);
            for (EdgeLine ed : edges) {
                if (!removedSoFar.contains(ed.fromId) && !removedSoFar.contains(ed.toId))
                    current.addEdge(ed.fromId, ed.toId);
            }
            int sinkId = current.findSink(); // calls YOUR Graph.findSink()

            if (sinkId == -1) {
                // Rule 3: no sink — cycle must exist
                AlgoStep s = new AlgoStep(removedSoFar, -1, "");
                Map<Integer, Integer> parentMap = new HashMap<>();
                int[] cycleEnds = findCycleByDFS(alive, removedSoFar, parentMap);
                if (cycleEnds != null) {
                    List<Integer> cyclePath = reconstructCycle(cycleEnds[0], cycleEnds[1], parentMap);
                    StringBuilder pathStr = new StringBuilder();
                    for (int i = 0; i < cyclePath.size(); i++) {
                        VertexNode vn = findVertex(cyclePath.get(i));
                        pathStr.append(vn != null ? vn.label : "?");
                        if (i < cyclePath.size() - 1) pathStr.append(" → ");
                    }
                    s.description = "No sink found — every remaining vertex has at least one outgoing edge. " +
                            "A cycle exists: " + pathStr;
                    s.cycleIds.addAll(cyclePath);
                    for (int i = 0; i < cyclePath.size() - 1; i++)
                        s.cycleEdges.add(new int[]{cyclePath.get(i), cyclePath.get(i + 1)});
                } else {
                    s.description = "No sink found — a cycle exists.";
                }
                s.verdict = "no";
                steps.add(s);
                break;
            }

            // Rule 4: remove the sink and record this step
            VertexNode sinkNode = findVertex(sinkId);
            removedSoFar = new HashSet<>(removedSoFar);
            removedSoFar.add(sinkId);
            steps.add(new AlgoStep(removedSoFar, sinkId,
                    "Vertex \"" + (sinkNode != null ? sinkNode.label : sinkId) +
                            "\" has no outgoing edges → it is a SINK. Removing it."));
        }

        // ── Step 3: Activate the step navigator in the GUI ────────────────────
        algoActive = true;
        stepIndex  = 0;
        btnRun.setVisible(false);
        btnReset.setVisible(true);
        btnNext.setEnabled(true);
        btnPrev.setEnabled(false);
        renderStep();
    }

    // ── DFS to find a cycle, fills parentMap with the discovery tree ───────
    int[] findCycleByDFS(List<VertexNode> alive, Set<Integer> removed, Map<Integer, Integer> parent) {
        Map<Integer, Integer> color = new HashMap<>(); // 0=white, 1=grey, 2=black
        for (VertexNode v : alive) color.put(v.id, 0);

        int[] result = new int[2]; // [cycleStart, cycleEnd]
        boolean[] found = {false};

        for (VertexNode v : alive) {
            if (color.get(v.id) == 0) {
                dfsCycle(v.id, alive, removed, color, parent, result, found);
                if (found[0]) return result;
            }
        }
        return null;
    }

    void dfsCycle(int v, List<VertexNode> alive, Set<Integer> removed,
                  Map<Integer, Integer> color, Map<Integer, Integer> parent,
                  int[] result, boolean[] found) {
        if (found[0]) return;
        color.put(v, 1); // mark grey (currently visiting)

        for (EdgeLine ed : edges) {
            if (ed.fromId != v) continue;
            if (removed.contains(ed.toId)) continue;
            if (alive.stream().noneMatch(n -> n.id == ed.toId)) continue;

            int nb = ed.toId;
            if (color.getOrDefault(nb, 0) == 1) {
                // Found a back edge — cycle!
                result[0] = nb; // cycle start
                result[1] = v;  // cycle end
                found[0]  = true;
                return;
            }
            if (color.getOrDefault(nb, 0) == 0) {
                parent.put(nb, v);
                dfsCycle(nb, alive, removed, color, parent, result, found);
                if (found[0]) return;
            }
        }
        color.put(v, 2); // mark black (fully processed)
    }

    List<Integer> reconstructCycle(int start, int end, Map<Integer, Integer> parent) {
        List<Integer> path = new ArrayList<>();
        path.add(start);
        int cur = end;
        int safety = 0;
        while (cur != start && safety++ < 50) {
            path.add(cur);
            Integer p = parent.get(cur);
            if (p == null) break;
            cur = p;
        }
        path.add(start);
        Collections.reverse(path);
        return path;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Step navigation
    // ══════════════════════════════════════════════════════════════════════
    void moveStep(int dir) {
        stepIndex = Math.max(0, Math.min(steps.size() - 1, stepIndex + dir));
        renderStep();
    }

    void renderStep() {
        if (stepIndex < 0 || stepIndex >= steps.size()) return;
        AlgoStep s = steps.get(stepIndex);

        // Update description
        descLabel.setText("<html>" + s.description + "</html>");

        // Update step counter
        stepCounter.setText("Step " + (stepIndex + 1) + " / " + steps.size());

        // Update nav buttons
        btnPrev.setEnabled(stepIndex > 0);
        btnNext.setEnabled(stepIndex < steps.size() - 1);

        // Update verdict badge
        if ("yes".equals(s.verdict)) {
            verdictLabel.setText("  YES — Acyclic  ");
            verdictLabel.setBackground(new Color(0xEAF3DE));
            verdictLabel.setForeground(new Color(0x27500A));
        } else if ("no".equals(s.verdict)) {
            verdictLabel.setText("  NO — Cycle found  ");
            verdictLabel.setBackground(new Color(0xFCEBEB));
            verdictLabel.setForeground(new Color(0x791F1F));
        } else {
            verdictLabel.setText(" ");
            verdictLabel.setBackground(Color.WHITE);
        }

        // Append to log if this step has something to log
        if (s.sinkId >= 0) {
            VertexNode sv = findVertex(s.sinkId);
            logArea.append("Sink removed: " + (sv != null ? sv.label : "?") + "\n");
        } else if ("yes".equals(s.verdict)) {
            logArea.append("Graph empty → YES (acyclic)\n");
        } else if ("no".equals(s.verdict)) {
            logArea.append("No sink found → cycle exists\n");
        }
        logArea.setCaretPosition(logArea.getDocument().getLength()); // scroll to bottom

        updateAdjPanel();
        canvas.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Update the adjacency list display
    // ══════════════════════════════════════════════════════════════════════
    void updateAdjPanel() {
        Set<Integer> removed = getCurrentRemoved();
        int currentSink      = getCurrentSink();
        StringBuilder sb     = new StringBuilder();

        for (VertexNode v : vertices) {
            if (removed.contains(v.id)) continue;
            List<String> outs = new ArrayList<>();
            for (EdgeLine ed : edges) {
                if (ed.fromId == v.id && !removed.contains(ed.toId)) {
                    VertexNode t = findVertex(ed.toId);
                    if (t != null) outs.add(t.label);
                }
            }
            sb.append(v.label).append(" → [").append(String.join(", ", outs)).append("]");
            if (v.id == currentSink) sb.append("  ← SINK");
            else if (outs.isEmpty()) sb.append("  ← no outgoing");
            sb.append("\n");
        }
        if (sb.isEmpty()) sb.append("(empty)");
        adjArea.setText(sb.toString());
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Reset — clears algorithm state, re-enables editing
    // ══════════════════════════════════════════════════════════════════════
    void resetAlgo() {
        steps.clear();
        stepIndex  = -1;
        algoActive = false;
        btnRun.setVisible(true);
        btnReset.setVisible(false);
        btnPrev.setEnabled(false);
        btnNext.setEnabled(false);
        stepCounter.setText("");
        verdictLabel.setText(" ");
        verdictLabel.setBackground(Color.WHITE);
        descLabel.setText("<html>Load a graph then click <b>Run algorithm</b>.</html>");
        logArea.setText("");
        updateAdjPanel();
        canvas.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Load preset graphs
    // ══════════════════════════════════════════════════════════════════════
    void loadPreset(String name) {
        vertices.clear(); edges.clear(); nextId = 0;

        if (name.equals("acyclic")) {
            // A→B, A→C, B→D, C→D, D→E  (no cycle)
            vertices.add(new VertexNode(0, "A",  80, 160));
            vertices.add(new VertexNode(1, "B", 220,  80));
            vertices.add(new VertexNode(2, "C", 220, 240));
            vertices.add(new VertexNode(3, "D", 360, 160));
            vertices.add(new VertexNode(4, "E", 470, 160));
            edges.add(new EdgeLine(0, 1));
            edges.add(new EdgeLine(0, 2));
            edges.add(new EdgeLine(1, 3));
            edges.add(new EdgeLine(2, 3));
            edges.add(new EdgeLine(3, 4));
            nextId = 5;

        } else if (name.equals("cyclic")) {
            // A→B, B→C, C→A, C→D  (cycle: A→B→C→A)
            vertices.add(new VertexNode(0, "A", 100, 100));
            vertices.add(new VertexNode(1, "B", 340,  80));
            vertices.add(new VertexNode(2, "C", 350, 230));
            vertices.add(new VertexNode(3, "D", 160, 250));
            edges.add(new EdgeLine(0, 1));
            edges.add(new EdgeLine(1, 2));
            edges.add(new EdgeLine(2, 0));
            edges.add(new EdgeLine(2, 3));
            nextId = 4;
        }
        refresh();
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Small helpers
    // ══════════════════════════════════════════════════════════════════════

    // Rebuild display without touching algorithm state
    void refresh() {
        updateAdjPanel();
        canvas.repaint();
    }

    // Return the vertex at a screen point, or null
    VertexNode vertexAt(Point p) {
        for (VertexNode v : vertices)
            if (Math.hypot(v.x - p.x, v.y - p.y) < RADIUS + 4) return v;
        return null;
    }

    // Return an edge whose drawn line passes near a point, or null
    EdgeLine edgeNear(Point p) {
        for (EdgeLine ed : edges) {
            VertexNode s = findVertex(ed.fromId), t = findVertex(ed.toId);
            if (s == null || t == null) continue;
            double dx = t.x - s.x, dy = t.y - s.y;
            double len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1) continue;
            double nx = dx / len, ny = dy / len;
            double px = p.x - s.x, py = p.y - s.y;
            double proj = px * nx + py * ny;
            double perpX = px - proj * nx, perpY = py - proj * ny;
            if (proj > 0 && proj < len && Math.sqrt(perpX * perpX + perpY * perpY) < 8) return ed;
        }
        return null;
    }

    VertexNode findVertex(int id) {
        return vertices.stream().filter(v -> v.id == id).findFirst().orElse(null);
    }

    // Convert a sequential number to a label: 0→A, 1→B, ..., 25→Z, 26→A2 ...
    String labelFor(int n) {
        String letter = String.valueOf((char) ('A' + (n % 26)));
        return n < 26 ? letter : letter + (n / 26);
    }

    // Get the set of removed vertices for the current step
    Set<Integer> getCurrentRemoved() {
        if (stepIndex >= 0 && stepIndex < steps.size()) return steps.get(stepIndex).removed;
        return new HashSet<>();
    }

    // Get the sink vertex ID for the current step (-1 if none)
    int getCurrentSink() {
        if (stepIndex >= 0 && stepIndex < steps.size()) return steps.get(stepIndex).sinkId;
        return -1;
    }

    // Get cycle vertex IDs for the current step
    Set<Integer> getCurrentCycleIds() {
        if (stepIndex >= 0 && stepIndex < steps.size()) return steps.get(stepIndex).cycleIds;
        return new HashSet<>();
    }

    // Get cycle edge keys ("fromId-toId") for highlighting
    Set<String> getCurrentCycleEdgeKeys() {
        Set<String> keys = new HashSet<>();
        if (stepIndex >= 0 && stepIndex < steps.size())
            for (int[] ce : steps.get(stepIndex).cycleEdges)
                keys.add(ce[0] + "-" + ce[1]);
        return keys;
    }

    // ── Small UI helpers ───────────────────────────────────────────────────

    JButton styledBtn(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setFocusPainted(false);
        return b;
    }

    JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.PLAIN, 12));
        l.setForeground(Color.GRAY);
        return l;
    }

    JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("SansSerif", Font.BOLD, 11));
        l.setForeground(Color.GRAY);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setBorder(new EmptyBorder(0, 0, 2, 0));
        return l;
    }

    // Create a mode toggle button (highlights when its mode is active)
    JButton modeBtn(String text, String modeVal) {
        JButton b = styledBtn(text);
        b.addActionListener(e -> {
            mode      = modeVal;
            edgeSource = null;
            canvas.repaint();
        });
        return b;
    }

    // ══════════════════════════════════════════════════════════════════════
    //  Entry point — launch the window
    // ══════════════════════════════════════════════════════════════════════
    public static void main(String[] args) {
        // SwingUtilities.invokeLater ensures the GUI is created on the
        // correct thread (the "Event Dispatch Thread" — EDT)
        SwingUtilities.invokeLater(GraphGUI::new);
    }
}