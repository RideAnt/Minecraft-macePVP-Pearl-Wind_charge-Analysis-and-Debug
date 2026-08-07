package com.example.pearlwindchargeanalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Text and first-person assist-line HUD.
 */
public final class HudRenderer {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GREEN = 0xFF55FF55;
    private static final int RED = 0xFFFF5555;
    private static final int YELLOW = 0xFFFFFF55;

    private static final double READY_BETA_TOLERANCE_DEG = 0.50;
    private static final double READY_HORIZONTAL_TOLERANCE_DEG = 0.50;
    private static final double NEAR_AIM_TOLERANCE_DEG = 0.75;
    private static final double FIXED_POINT_MAX_THROWER_DRIFT_BLOCKS = 0.80;

    private static final int AIM_HORIZONTAL_HALF_LENGTH = 155;
    private static final int AIM_VERTICAL_HALF_LENGTH = 78;
    private static final int AIM_CENTER_GAP = 18;
    private static final int AIM_EDGE_PADDING = 4;
    private static final int AIM_MERGE_DISTANCE_PX = 5;
    private static final int VANILLA_CROSSHAIR_SIZE = 15;

    private static final String[] AIM_CIRCLE_MASK = {
            "..XXX..",
            ".X...X.",
            "X.....X",
            "X.....X",
            "X.....X",
            ".X...X.",
            "..XXX.."
    };

    private HudRenderer() {
    }

    public static void render(GuiGraphicsExtractor context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();

        if (client.player == null || client.level == null || !ModConfig.get().enabled) {
            return;
        }

        boolean holdingPearl = isHolding(client, Items.ENDER_PEARL);
        boolean holdingWindCharge = isHolding(client, Items.WIND_CHARGE);
        float tickProgress = tickCounter.getGameTimeDeltaPartialTick(false);

        List<Line> lines = new ArrayList<>();

        if (PearlWindChargeAnalysisClient.getModeNoticeTicks() > 0) {
            lines.add(new Line(PearlWindChargeAnalysisClient.getModeNoticeText(), WHITE));
        }

        if (!holdingPearl && !holdingWindCharge) {
            drawLines(context, client, lines);
            return;
        }

        if (holdingPearl) {
            addPearlLines(client, tickProgress, lines);
        }

        if (holdingWindCharge) {
            addFixedWindChargeLines(client, tickProgress, lines);

            if (ModConfig.get().showAssistHud) {
                drawFixedPointAimCues(context, client, tickProgress);
            }
        }

        drawLines(context, client, lines);
    }

    private static void addPearlLines(
            Minecraft client,
            float tickProgress,
            List<Line> lines
    ) {
        double currentAlpha = AngleCalculator.getLookElevationDeg(client.player);
        lines.add(new Line(format("Pearl α: %.2f°", currentAlpha), WHITE));

        List<Line> pearlLines = new ArrayList<>();
        boolean more = false;
        int index = 1;

        for (PearlTrack track : PearlWindChargeAnalysisClient.TRACKER.getFlyingPearls()) {
            if (index > 2) {
                more = true;
                break;
            }

            double ageTick = PearlWindChargeAnalysisClient.TRACKER.getAgeTicks(
                    client,
                    track,
                    tickProgress
            );

            if (ageTick >= 0.0) {
                pearlLines.add(new Line(format(
                        "P%d α=%.2f°  %.2ft / %.2fs",
                        index,
                        track.actualAlphaDeg(),
                        ageTick,
                        ageTick / 20.0
                ), WHITE));
            }

            index++;
        }

        if (more && !pearlLines.isEmpty()) {
            Line last = pearlLines.get(pearlLines.size() - 1);
            pearlLines.set(
                    pearlLines.size() - 1,
                    new Line(last.text + "  +more", YELLOW)
            );
        }

        lines.addAll(pearlLines);
    }

    private static void addFixedWindChargeLines(
            Minecraft client,
            float tickProgress,
            List<Line> lines
    ) {
        double currentBeta = AngleCalculator.getLookElevationDeg(client.player);
        double currentGamma = AngleCalculator.getGameYawDeg(client.player);
        AnalysisMode mode = PearlWindChargeAnalysisClient.getAnalysisMode();
        AnalysisEntry selected = selectDisplayedPearl(client, tickProgress);

        if (selected == null) {
            if (mode == AnalysisMode.MOVING_POINT) {
                lines.add(new Line(format(
                        "Wind β：%.2f° γ：%.2f°",
                        currentBeta,
                        currentGamma
                ), WHITE));
            } else {
                lines.add(new Line(format(
                        "Wind β：%.2f°",
                        currentBeta
                ), WHITE));
            }
            return;
        }

        ModConfig.Config config = ModConfig.get();
        FixedPointSolution solution = selected.solution;

        /*
         * Highest-priority rule: a valid Best beta outside the configured Best
         * range falls back to the pearl-information HUD.
         */
        if (solution.isValid()
                && !AngleCalculator.isBetaInsideRange(
                        solution.betaDeg(),
                        config.bestBetaMin,
                        config.bestBetaMax
                )) {
            addPearlLines(client, tickProgress, lines);
            return;
        }

        if (mode == AnalysisMode.FIXED_POINT) {
            double drift = selected.track.distanceFromThrowerStart(
                    client.player
            );
            if (drift > FIXED_POINT_MAX_THROWER_DRIFT_BLOCKS) {
                addCurrentWindLine(
                        lines,
                        selected.index,
                        currentBeta,
                        currentGamma,
                        mode,
                        RED
                );
                lines.add(new Line(
                        format("P%d：超出投掷点范围", selected.index),
                        RED
                ));

                if (solution.isValid()) {
                    double betaError =
                            Math.abs(currentBeta - solution.betaDeg());
                    lines.add(new Line(format(
                            "Δβ：%.2f°  Δγ：%.2f°",
                            betaError,
                            Math.abs(solution.signedYawDeltaDeg())
                    ), RED));
                } else {
                    lines.add(new Line("Δβ：--  Δγ：--", RED));
                }
                return;
            }
        }

        if (!solution.isValid()) {
            String message = switch (solution.status()) {
                case NON_AIR_PATH, BLOCKED_PATH -> "无法解析";
                case NO_SOLUTION, VALID -> "超出解析范围";
            };

            addCurrentWindLine(
                    lines,
                    selected.index,
                    currentBeta,
                    currentGamma,
                    mode,
                    RED
            );
            lines.add(new Line(
                    format("P%d：%s", selected.index, message),
                    RED
            ));
            lines.add(new Line("Δβ：--  Δγ：--", RED));
            return;
        }

        double betaError = Math.abs(currentBeta - solution.betaDeg());
        double gammaError = Math.abs(solution.signedYawDeltaDeg());

        if (betaError > config.verticalAimMax
                || gammaError > config.horizontalAimMax) {
            addCurrentWindLine(
                    lines,
                    selected.index,
                    currentBeta,
                    currentGamma,
                    mode,
                    RED
            );
            lines.add(new Line(
                    format("P%d：超出投掷角范围", selected.index),
                    RED
            ));
            lines.add(new Line(format(
                    "Δβ：%.2f°  Δγ：%.2f°",
                    betaError,
                    gammaError
            ), RED));
            return;
        }

        int color = getAimColor(betaError, gammaError);
        boolean ready = color == GREEN;

        addCurrentWindLine(
                lines,
                selected.index,
                currentBeta,
                currentGamma,
                mode,
                color
        );
        addBestPearlLine(lines, selected, mode, color);
        lines.add(new Line(
                ready
                        ? format(
                                "READY  Δβ：%.2f°  Δγ：%.2f°",
                                betaError,
                                gammaError
                        )
                        : format(
                                "Δβ：%.2f°  Δγ：%.2f°",
                                betaError,
                                gammaError
                        ),
                color
        ));
    }

    private static void addCurrentWindLine(
            List<Line> lines,
            int index,
            double currentBeta,
            double currentGamma,
            AnalysisMode mode,
            int color
    ) {
        if (mode == AnalysisMode.MOVING_POINT) {
            lines.add(new Line(format(
                    "W%d β：%.2f° γ：%.2f°",
                    index,
                    currentBeta,
                    currentGamma
            ), color));
            return;
        }

        lines.add(new Line(format(
                "W%d β：%.2f°",
                index,
                currentBeta
        ), color));
    }

    private static void addBestPearlLine(
            List<Line> lines,
            AnalysisEntry selected,
            AnalysisMode mode,
            int color
    ) {
        if (mode == AnalysisMode.MOVING_POINT) {
            lines.add(new Line(format(
                    "P%d β：%.2f° γ：%.2f°",
                    selected.index,
                    selected.solution.betaDeg(),
                    selected.solution.targetWorldYawDeg()
            ), color));
            return;
        }

        lines.add(new Line(format(
                "P%d β：%.2f°",
                selected.index,
                selected.solution.betaDeg()
        ), color));
    }

    private static void drawFixedPointAimCues(
            GuiGraphicsExtractor context,
            Minecraft client,
            float tickProgress
    ) {
        double currentBeta = AngleCalculator.getLookElevationDeg(client.player);
        ModConfig.Config config = ModConfig.get();
        AnalysisEntry selected = selectDisplayedPearl(client, tickProgress);
        List<AimCueTarget> targets = new ArrayList<>(2);

        for (AnalysisEntry entry : analyzeFirstTwoPearls(client, tickProgress)) {
            FixedPointSolution solution = entry.solution;

            if (!solution.isValid()) {
                continue;
            }

            if (PearlWindChargeAnalysisClient.getAnalysisMode()
                    == AnalysisMode.FIXED_POINT
                    && entry.track.distanceFromThrowerStart(client.player)
                    > FIXED_POINT_MAX_THROWER_DRIFT_BLOCKS) {
                continue;
            }

            if (!AngleCalculator.isBetaInsideRange(
                    solution.betaDeg(),
                    config.bestBetaMin,
                    config.bestBetaMax
            )) {
                continue;
            }

            double betaError = Math.abs(currentBeta - solution.betaDeg());
            double gammaError = Math.abs(solution.signedYawDeltaDeg());

            if (betaError > config.verticalAimMax
                    || gammaError > config.horizontalAimMax) {
                continue;
            }

            int color = getAimColor(betaError, gammaError);
            ScreenPoint point = projectAimDirection(
                    client,
                    context.guiWidth(),
                    context.guiHeight(),
                    config.assistLineMode,
                    solution.betaDeg(),
                    solution.targetWorldYawDeg()
            );
            if (point == null || !isPointVisible(context, point)) {
                continue;
            }

            targets.add(new AimCueTarget(
                    "P" + entry.index,
                    point,
                    color,
                    selected != null
                            && selected.track.uuid().equals(entry.track.uuid())
            ));
        }

        if (targets.size() == 2
                && squaredDistance(targets.get(0).point, targets.get(1).point)
                <= AIM_MERGE_DISTANCE_PX * AIM_MERGE_DISTANCE_PX) {
            AimCueTarget first = targets.get(0);
            AimCueTarget second = targets.get(1);
            AimCueTarget preferred = second.selected ? second : first;
            ScreenPoint mergedPoint = new ScreenPoint(
                    (first.point.x + second.point.x) / 2,
                    (first.point.y + second.point.y) / 2
            );
            drawAimCueAt(
                    context,
                    client,
                    first.label + "/" + second.label,
                    mergedPoint,
                    preferred.color
            );
            return;
        }

        for (AimCueTarget target : targets) {
            if (!target.selected) {
                drawAimCueAt(
                        context,
                        client,
                        target.label,
                        target.point,
                        target.color
                );
            }
        }
        for (AimCueTarget target : targets) {
            if (target.selected) {
                drawAimCueAt(
                        context,
                        client,
                        target.label,
                        target.point,
                        target.color
                );
            }
        }
    }

    private static AnalysisEntry selectDisplayedPearl(
            Minecraft client,
            float tickProgress
    ) {
        List<AnalysisEntry> entries = analyzeFirstTwoPearls(client, tickProgress);
        if (entries.isEmpty()) {
            return null;
        }

        AnalysisEntry bestValid = null;
        for (AnalysisEntry entry : entries) {
            if (!entry.solution.isValid()) {
                continue;
            }

            if (bestValid == null
                    || Math.abs(entry.solution.signedYawDeltaDeg())
                    < Math.abs(bestValid.solution.signedYawDeltaDeg())) {
                bestValid = entry;
            }
        }

        return bestValid != null ? bestValid : entries.getFirst();
    }

    private static List<AnalysisEntry> analyzeFirstTwoPearls(
            Minecraft client,
            float tickProgress
    ) {
        List<AnalysisEntry> entries = new ArrayList<>(2);
        int index = 1;

        for (PearlTrack track : PearlWindChargeAnalysisClient.TRACKER.getFlyingPearls()) {
            if (index > 2) {
                break;
            }

            double clientAgeTick = PearlWindChargeAnalysisClient.TRACKER.getAgeTicks(
                    client,
                    track,
                    tickProgress
            );

            if (clientAgeTick >= 0.0) {
                double effectiveAgeTick =
                        NetworkTimingCompensation.getEffectivePearlAgeTicks(
                                client,
                                clientAgeTick
                        );

                FixedPointSolution solution = FixedPoint3DSolver.solve(
                        client,
                        track,
                        effectiveAgeTick
                );

                entries.add(new AnalysisEntry(
                        index,
                        track,
                        clientAgeTick,
                        effectiveAgeTick,
                        solution
                ));
            }

            index++;
        }

        return entries;
    }

    private static int getAimColor(double betaErrorDeg, double gammaErrorDeg) {
        if (betaErrorDeg <= READY_BETA_TOLERANCE_DEG
                && gammaErrorDeg <= READY_HORIZONTAL_TOLERANCE_DEG) {
            return GREEN;
        }

        if (betaErrorDeg <= NEAR_AIM_TOLERANCE_DEG
                && gammaErrorDeg <= NEAR_AIM_TOLERANCE_DEG) {
            return YELLOW;
        }

        return RED;
    }

    private static void drawAimCueAt(
            GuiGraphicsExtractor context,
            Minecraft client,
            String label,
            ScreenPoint targetPoint,
            int color
    ) {
        int x = targetPoint.x();
        int y = targetPoint.y();
        boolean independent = ModConfig.get().assistLineMode
                == AssistLineMode.INDEPENDENT_AXES;
        int horizontalCenterX = independent
                ? getVanillaCrosshairCenterX(context.guiWidth())
                : x;
        int verticalCenterY = independent
                ? getVanillaCrosshairCenterY(context.guiHeight())
                : y;

        drawBestBetaHorizontalCue(
                context,
                client,
                label,
                horizontalCenterX,
                y,
                color
        );
        drawTargetYawVerticalCue(
                context,
                client,
                label,
                x,
                verticalCenterY,
                color
        );
        drawCircleOutline(context, x, y, color);
    }

    /**
     * Independent mode projects yaw and elevation as separate one-dimensional
     * axes. Perspective mode projects the complete target direction.
     */
    private static ScreenPoint projectAimDirection(
            Minecraft client,
            int width,
            int height,
            AssistLineMode mode,
            double targetBetaDeg,
            double targetWorldYawDeg
    ) {
        if (mode == AssistLineMode.INDEPENDENT_AXES) {
            return projectIndependentAxes(
                    client,
                    width,
                    height,
                    targetBetaDeg,
                    targetWorldYawDeg
            );
        }

        return projectPerspectiveDirection(
                client,
                width,
                height,
                targetBetaDeg,
                targetWorldYawDeg
        );
    }

    private static ScreenPoint projectIndependentAxes(
            Minecraft client,
            int width,
            int height,
            double targetBetaDeg,
            double targetWorldYawDeg
    ) {
        Camera camera = client.gameRenderer.mainCamera();
        double yawDelta = Mth.wrapDegrees(
                targetWorldYawDeg - camera.yRot()
        );
        double betaDelta = targetBetaDeg + camera.xRot();

        if (Math.abs(yawDelta) >= 89.9 || Math.abs(betaDelta) >= 89.9) {
            return null;
        }

        double focalLength = getFocalLength(
                client,
                height
        );
        if (!Double.isFinite(focalLength)) {
            return null;
        }

        double projectedX = getVanillaCrosshairCenterX(width)
                + Math.tan(Math.toRadians(yawDelta)) * focalLength;
        double projectedY = getVanillaCrosshairCenterY(height)
                - Math.tan(Math.toRadians(betaDelta)) * focalLength;

        if (!Double.isFinite(projectedX) || !Double.isFinite(projectedY)) {
            return null;
        }

        return new ScreenPoint(
                (int) Math.round(projectedX),
                (int) Math.round(projectedY)
        );
    }

    private static ScreenPoint projectPerspectiveDirection(
            Minecraft client,
            int width,
            int height,
            double targetBetaDeg,
            double targetWorldYawDeg
    ) {
        Camera camera = client.gameRenderer.mainCamera();
        double cameraYawDeg = camera.yRot();
        double cameraBetaDeg = -camera.xRot();

        Vec3 forward = AngleCalculator.getDirectionFromGameAnglesDeg(
                cameraBetaDeg,
                cameraYawDeg
        );

        double cameraYawRad = Math.toRadians(cameraYawDeg);
        Vec3 right = new Vec3(
                -Math.cos(cameraYawRad),
                0.0,
                -Math.sin(cameraYawRad)
        );
        Vec3 up = right.cross(forward).normalize();

        Vec3 targetDirection =
                AngleCalculator.getDirectionFromGameAnglesDeg(
                        targetBetaDeg,
                        targetWorldYawDeg
                );

        double cameraX = targetDirection.dot(right);
        double cameraY = targetDirection.dot(up);
        double cameraZ = targetDirection.dot(forward);

        if (!Double.isFinite(cameraZ) || cameraZ <= 1.0E-6) {
            return null;
        }

        double focalLength = getFocalLength(
                client,
                height
        );
        if (!Double.isFinite(focalLength)) {
            return null;
        }

        double projectedX = getVanillaCrosshairCenterX(width)
                + cameraX / cameraZ * focalLength;
        double projectedY = getVanillaCrosshairCenterY(height)
                - cameraY / cameraZ * focalLength;

        if (!Double.isFinite(projectedX) || !Double.isFinite(projectedY)) {
            return null;
        }

        return new ScreenPoint(
                (int) Math.round(projectedX),
                (int) Math.round(projectedY)
        );
    }

    private static double getFocalLength(
            Minecraft client,
            int height
    ) {
        double fovDeg = getRenderedFovDeg(client);
        if (!Double.isFinite(fovDeg)) {
            return Double.NaN;
        }

        fovDeg = Math.max(1.0, Math.min(179.0, fovDeg));
        return (height * 0.5) / Math.tan(Math.toRadians(fovDeg) * 0.5);
    }

    private static boolean isPointVisible(
            GuiGraphicsExtractor context,
            ScreenPoint point
    ) {
        int width = context.guiWidth();
        int height = context.guiHeight();
        return point.x >= AIM_EDGE_PADDING
                && point.x <= width - AIM_EDGE_PADDING
                && point.y >= AIM_EDGE_PADDING
                && point.y <= height - AIM_EDGE_PADDING;
    }

    private static int squaredDistance(ScreenPoint first, ScreenPoint second) {
        int dx = first.x - second.x;
        int dy = first.y - second.y;
        return dx * dx + dy * dy;
    }

    /**
     * Reads the rendered vertical FOV from the level projection matrix.
     *
     * <p>26.2 removed {@code GameRenderer.getFov}; the FOV now lives baked into
     * {@link net.minecraft.client.renderer.state.level.CameraRenderState#projectionMatrix}.
     * For a perspective projection {@code m11 = 1/tan(fovY/2)}, so
     * {@code fovY = 2*atan(1/m11)}.
     */
    private static double getRenderedFovDeg(Minecraft client) {
        try {
            Matrix4f projection = client.gameRenderer
                    .gameRenderState()
                    .levelRenderState
                    .cameraRenderState
                    .projectionMatrix;
            float m11 = projection.m11();
            if (Float.isFinite(m11) && m11 > 0.0F) {
                return Math.toDegrees(2.0 * Math.atan(1.0 / m11));
            }
        } catch (NullPointerException ignored) {
            // Render state not yet populated this frame; fall back to configured FOV.
        }
        return client.options.fov().get();
    }

    private static void drawBestBetaHorizontalCue(
            GuiGraphicsExtractor context,
            Minecraft client,
            String label,
            int targetX,
            int targetY,
            int color
    ) {
        int width = context.guiWidth();

        int leftStart = Math.max(
                AIM_EDGE_PADDING,
                targetX - AIM_HORIZONTAL_HALF_LENGTH
        );
        int leftEnd = Math.max(leftStart, targetX - AIM_CENTER_GAP);
        int rightStart = Math.min(
                width - AIM_EDGE_PADDING,
                targetX + AIM_CENTER_GAP
        );
        int rightEnd = Math.min(
                width - AIM_EDGE_PADDING,
                targetX + AIM_HORIZONTAL_HALF_LENGTH
        );

        if (leftEnd > leftStart) {
            context.fill(leftStart, targetY, leftEnd, targetY + 1, color);
        }

        if (rightEnd > rightStart) {
            context.fill(rightStart, targetY, rightEnd, targetY + 1, color);
        }

        int textWidth = client.font.width(label);
        int textY = Math.max(AIM_EDGE_PADDING, targetY - 10);

        context.text(
                client.font,
                Component.literal(label),
                leftStart,
                textY,
                color
        );
        context.text(
                client.font,
                Component.literal(label),
                Math.max(
                        AIM_EDGE_PADDING,
                        rightEnd - textWidth
                ),
                textY,
                color
        );
    }

    private static void drawTargetYawVerticalCue(
            GuiGraphicsExtractor context,
            Minecraft client,
            String label,
            int targetX,
            int targetY,
            int color
    ) {
        int width = context.guiWidth();
        int height = context.guiHeight();

        int topStart = Math.max(
                AIM_EDGE_PADDING,
                targetY - AIM_VERTICAL_HALF_LENGTH
        );
        int topEnd = Math.max(topStart, targetY - AIM_CENTER_GAP);
        int bottomStart = Math.min(
                height - AIM_EDGE_PADDING,
                targetY + AIM_CENTER_GAP
        );
        int bottomEnd = Math.min(
                height - AIM_EDGE_PADDING,
                targetY + AIM_VERTICAL_HALF_LENGTH
        );

        if (topEnd > topStart) {
            context.fill(targetX, topStart, targetX + 1, topEnd, color);
        }

        if (bottomEnd > bottomStart) {
            context.fill(
                    targetX,
                    bottomStart,
                    targetX + 1,
                    bottomEnd,
                    color
            );
        }

        int textWidth = client.font.width(label);
        int textX = Math.max(
                AIM_EDGE_PADDING,
                Math.min(
                        width - AIM_EDGE_PADDING - textWidth,
                        targetX - textWidth / 2
                )
        );

        context.text(
                client.font,
                Component.literal(label),
                textX,
                Math.max(AIM_EDGE_PADDING, topStart - 10),
                color
        );
        context.text(
                client.font,
                Component.literal(label),
                textX,
                Math.min(height - AIM_EDGE_PADDING - 10, bottomEnd + 2),
                color
        );
    }

    private static void drawCircleOutline(
            GuiGraphicsExtractor context,
            int centerX,
            int centerY,
            int color
    ) {
        int top = centerY - 3;
        int left = centerX - 3;

        for (int row = 0; row < AIM_CIRCLE_MASK.length; row++) {
            String maskLine = AIM_CIRCLE_MASK[row];

            for (int col = 0; col < maskLine.length(); col++) {
                if (maskLine.charAt(col) == 'X') {
                    int x = left + col;
                    int y = top + row;
                    context.fill(x, y, x + 1, y + 1, color);
                }
            }
        }
    }

    private static int getVanillaCrosshairCenterX(int width) {
        return (width - VANILLA_CROSSHAIR_SIZE) / 2
                + VANILLA_CROSSHAIR_SIZE / 2;
    }

    private static int getVanillaCrosshairCenterY(int height) {
        return (height - VANILLA_CROSSHAIR_SIZE) / 2
                + VANILLA_CROSSHAIR_SIZE / 2;
    }

    private static boolean isHolding(Minecraft client, Item item) {
        return client.player != null
                && (
                client.player.getMainHandItem().is(item)
                        || client.player.getOffhandItem().is(item)
        );
    }

    private static void drawLines(
            GuiGraphicsExtractor context,
            Minecraft client,
            List<Line> lines
    ) {
        if (lines.isEmpty()) {
            return;
        }

        int centerX = context.guiWidth() / 2;
        int bottomY = context.guiHeight() - 58;
        int baseY = bottomY - Math.max(0, lines.size() - 1) * 10;

        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int y = baseY + i * 10;
            int textWidth = client.font.width(line.text);

            context.text(
                    client.font,
                    Component.literal(line.text),
                    centerX - textWidth / 2,
                    y,
                    line.color
            );
        }
    }

    private static String format(String pattern, Object... args) {
        return String.format(Locale.ROOT, pattern, args);
    }

    private record ScreenPoint(int x, int y) {
    }

    private record AimCueTarget(
            String label,
            ScreenPoint point,
            int color,
            boolean selected
    ) {
    }

    private record Line(String text, int color) {
    }

    private record AnalysisEntry(
            int index,
            PearlTrack track,
            double clientAgeTick,
            double effectiveAgeTick,
            FixedPointSolution solution
    ) {
    }
}
