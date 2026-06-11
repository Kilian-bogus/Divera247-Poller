package de.main.divera247.service;

import de.main.divera247.model.AlarmResponse;
import de.main.divera247.model.LastAlarm;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlertService {
    private static final Logger log = LoggerFactory.getLogger(AlertService.class);
    private static final Color DARK_BACKGROUND = new Color(3, 13, 23);
    private static final Color DARK_SURFACE = new Color(7, 22, 36);
    private static final Color DARK_SURFACE_ALT = new Color(10, 31, 50);
    private static final Color DARK_BORDER = new Color(27, 58, 82);
    private static final Color DARK_TEXT = new Color(246, 248, 252);
    private static final Color DARK_MUTED_TEXT = new Color(169, 181, 199);
    private static final Color LIGHT_BACKGROUND = new Color(248, 250, 252);
    private static final Color LIGHT_SURFACE = Color.WHITE;
    private static final Color LIGHT_SURFACE_ALT = new Color(235, 239, 244);
    private static final Color LIGHT_BORDER = new Color(215, 222, 230);
    private static final Color LIGHT_TEXT = new Color(25, 30, 35);
    private static final Color LIGHT_MUTED_TEXT = new Color(85, 95, 110);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    private static final Pattern LAT_PATTERN = Pattern.compile("\"lat\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern LON_PATTERN = Pattern.compile("\"lon\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern COORDINATE_PATTERN = Pattern.compile("(-?\\d{1,2}[.,]\\d+)\\s*[,; ]\\s*(-?\\d{1,3}[.,]\\d+)");
    private final SoundService soundService;
    private final ConfigService.AppConfig config;

    public AlertService(SoundService soundService) {
        this.soundService = soundService;
        this.config = new ConfigService().loadOrCreate();
    }

    public void alert(AlarmResponse alarmResponse) {
        if (alarmResponse == null || !alarmResponse.isSuccess() || alarmResponse.getData() == null) {
            log.debug("Alarm is not successful, skipping alert.");
            return;
        }

        LastAlarm alarm = alarmResponse.getData();
        log.info("Alert for last alarm: {}", alarmResponse);
        showPopup(alarm);
    }

    private void showPopup(LastAlarm alarm) {
        SoundService.ToneHandle toneHandle = null;
        try {
            toneHandle = alarm.isPriority() ? soundService.playPriority() : soundService.playInfo();
            showAlarmDashboard(alarm);
        } finally {
            if (toneHandle != null) {
                toneHandle.close();
            }
        }
    }

    private void showAlarmDashboard(LastAlarm alarm) {
        Color accent = alarm.isPriority() ? new Color(255, 72, 65) : new Color(24, 129, 255);
        Color blue = new Color(24, 129, 255);
        JDialog dialog = new JDialog((Frame) null, valueOrFallback(alarm.getTitle(), text("Divera247 Einsatz", "Divera247 Alert")), true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setUndecorated(true);

        RoundedPanel root = new RoundedPanel(new BorderLayout(14, 0), backgroundColor(), borderColor(), 18);
        root.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JLabel mapLabel = new JLabel(text("Karte wird geladen...", "Loading map..."), SwingConstants.CENTER);
        mapLabel.setOpaque(true);
        mapLabel.setBackground(new Color(2, 15, 26));
        mapLabel.setForeground(mutedTextColor());
        mapLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        mapLabel.setPreferredSize(new Dimension(650, 680));

        JPanel mapPane = new JPanel(new BorderLayout());
        mapPane.setOpaque(false);
        mapPane.add(mapLabel, BorderLayout.CENTER);
        mapPane.add(clockPanel(), BorderLayout.NORTH);

        RoundedPanel details = new RoundedPanel(new BorderLayout(0, 18), surfaceColor(), accent, 20);
        details.setPreferredSize(new Dimension(500, 680));
        details.setBorder(BorderFactory.createEmptyBorder(26, 24, 24, 24));
        details.add(headerPanel(alarm, dialog, accent), BorderLayout.NORTH);
        details.add(detailBody(alarm, accent, blue), BorderLayout.CENTER);
        details.add(actionPanel(dialog, blue), BorderLayout.SOUTH);

        root.add(mapPane, BorderLayout.CENTER);
        root.add(details, BorderLayout.EAST);

        loadDashboardMap(alarm, mapLabel);

        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(1120, 720));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.setVisible(true);
    }

    private JPanel clockPanel() {
        LocalDateTime now = LocalDateTime.now();
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 10));

        JLabel icon = new JLabel("O", SwingConstants.CENTER);
        icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        icon.setForeground(mutedTextColor());

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 0));
        textPanel.setOpaque(false);
        JLabel time = new JLabel(now.format(DateTimeFormatter.ofPattern("HH:mm:ss")), SwingConstants.RIGHT);
        time.setForeground(textColor());
        time.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        JLabel date = new JLabel(now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), SwingConstants.RIGHT);
        date.setForeground(mutedTextColor());
        date.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        textPanel.add(time);
        textPanel.add(date);

        panel.add(icon, BorderLayout.EAST);
        panel.add(textPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel headerPanel(LastAlarm alarm, JDialog dialog, Color accent) {
        JPanel panel = new JPanel(new BorderLayout(14, 0));
        panel.setOpaque(false);

        JLabel icon = new JLabel(alarm.isPriority() ? "!" : "i", SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(42, 42));
        icon.setForeground(accent);
        icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));

        JPanel titles = new JPanel(new GridLayout(2, 1, 0, 8));
        titles.setOpaque(false);
        JLabel kicker = new JLabel(alarm.isPriority() ? text("EINSATZ GEFUNDEN", "ALERT FOUND") : text("INFORMATION", "INFORMATION"));
        kicker.setForeground(accent);
        kicker.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        JLabel title = new JLabel(valueOrFallback(alarm.getTitle(), text("Neuer Einsatz", "New Alert")));
        title.setForeground(textColor());
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 27));
        titles.add(kicker);
        titles.add(title);

        JButton close = new JButton("x");
        close.setContentAreaFilled(false);
        close.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
        close.setFocusPainted(false);
        close.setForeground(mutedTextColor());
        close.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 28));
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(event -> dialog.dispose());

        panel.add(icon, BorderLayout.WEST);
        panel.add(titles, BorderLayout.CENTER);
        panel.add(close, BorderLayout.EAST);
        return panel;
    }

    private JPanel detailBody(LastAlarm alarm, Color accent, Color blue) {
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.insets = new Insets(0, 0, 14, 0);

        addDashboardSection(body, constraints, 0, text("SICHTWACHE", "LOCATION"),
                valueOrFallback(alarm.getAddress(), "-"), "", blue, "#");
        addDashboardSection(body, constraints, 1, text("BESCHREIBUNG", "DESCRIPTION"),
                valueOrFallback(alarm.getText(), "-"), "", blue, "");
        addDashboardSection(body, constraints, 2, text("PRIORITAET", "PRIORITY"),
                alarm.isPriority() ? text("Hoch", "High") : text("Normal", "Normal"),
                alarm.isPriority() ? text("Dringender Einsatz", "Urgent alert") : text("Standardmeldung", "Standard alert"),
                accent, "!");

        JPanel meta = new JPanel(new GridLayout(2, 2, 12, 12));
        meta.setOpaque(false);
        meta.add(infoTile("#", text("EINSATZNUMMER", "INCIDENT NO."), text("Aktueller Einsatz", "Current alert")));
        meta.add(infoTile("!", text("ALARMIERUNG", "ALERTED"), text("Gerade eben", "Just now")));
        meta.add(infoTile("[]", text("STICHWORT", "KEYWORD"), valueOrFallback(alarm.getTitle(), "-")));
        meta.add(infoTile("=", text("QUELLE", "SOURCE"), "Divera API"));
        constraints.gridy = 3;
        body.add(meta, constraints);

        return body;
    }

    private void addDashboardSection(JPanel panel, GridBagConstraints constraints, int row, String label, String value, String subValue, Color accent, String iconText) {
        JPanel section = new JPanel(new BorderLayout(0, 8));
        section.setOpaque(false);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(accent);
        labelComponent.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));

        RoundedPanel card = new RoundedPanel(new BorderLayout(14, 0), surfaceAltColor(), borderColor(), 8);
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        if (!isBlank(iconText)) {
            JLabel icon = new JLabel(iconText, SwingConstants.CENTER);
            icon.setPreferredSize(new Dimension(34, 34));
            icon.setForeground(accent);
            icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            card.add(icon, BorderLayout.WEST);
        }

        JPanel copy = new JPanel(new GridLayout(isBlank(subValue) ? 1 : 2, 1, 0, 4));
        copy.setOpaque(false);
        JLabel valueComponent = new JLabel(toHtml(value, 330));
        valueComponent.setForeground(textColor());
        valueComponent.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        copy.add(valueComponent);
        if (!isBlank(subValue)) {
            JLabel sub = new JLabel(subValue);
            sub.setForeground(mutedTextColor());
            sub.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
            copy.add(sub);
        }
        card.add(copy, BorderLayout.CENTER);

        section.add(labelComponent, BorderLayout.NORTH);
        section.add(card, BorderLayout.CENTER);
        constraints.gridy = row;
        panel.add(section, constraints);
    }

    private JPanel infoTile(String iconText, String label, String value) {
        RoundedPanel tile = new RoundedPanel(new BorderLayout(12, 0), surfaceAltColor(), borderColor(), 8);
        tile.setBorder(BorderFactory.createEmptyBorder(13, 14, 13, 14));
        JLabel icon = new JLabel(iconText, SwingConstants.CENTER);
        icon.setPreferredSize(new Dimension(30, 30));
        icon.setForeground(mutedTextColor());
        icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));

        JPanel texts = new JPanel(new GridLayout(2, 1, 0, 3));
        texts.setOpaque(false);
        JLabel labelComponent = new JLabel(label);
        labelComponent.setForeground(mutedTextColor());
        labelComponent.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        JLabel valueComponent = new JLabel(toHtml(value, 140));
        valueComponent.setForeground(textColor());
        valueComponent.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        texts.add(labelComponent);
        texts.add(valueComponent);

        tile.add(icon, BorderLayout.WEST);
        tile.add(texts, BorderLayout.CENTER);
        return tile;
    }

    private JPanel actionPanel(JDialog dialog, Color blue) {
        JPanel actions = new JPanel(new GridLayout(1, 2, 14, 0));
        actions.setOpaque(false);
        JButton map = actionButton(text("AUF KARTE ANZEIGEN", "SHOW ON MAP"), blue, Color.WHITE, true);
        JButton open = actionButton(text("EINSATZ OEFFNEN", "OPEN ALERT"), surfaceAltColor(), textColor(), false);
        open.addActionListener(event -> dialog.dispose());
        actions.add(map);
        actions.add(open);
        return actions;
    }

    private JButton actionButton(String label, Color background, Color foreground, boolean filled) {
        JButton button = new JButton(label);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 15));
        button.setFocusPainted(false);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (!filled) {
            button.setContentAreaFilled(false);
        }
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        return button;
    }

    private String toHtml(String value, int width) {
        return "<html><body style='width:" + width + "px'>" + escapeHtml(value) + "</body></html>";
    }

    private String escapeHtml(String value) {
        return valueOrFallback(value, "").replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void loadDashboardMap(LastAlarm alarm, JLabel mapLabel) {
        String address = valueOrFallback(alarm.getAddress(), "");
        if (isBlank(address)) {
            mapLabel.setText(text("Keine Adresse vorhanden", "No address available"));
            return;
        }

        Thread mapThread = new Thread(() -> {
            try {
                Coordinates coordinates = findCoordinates(alarm);
                BufferedImage map = renderDashboardMap(coordinates, config.defaultMapZoom(), 650, 680);
                SwingUtilities.invokeLater(() -> {
                    mapLabel.setText(null);
                    mapLabel.setIcon(new ImageIcon(map));
                });
            } catch (Exception exception) {
                log.warn("Failed to load map for address: {}", alarm.getAddress(), exception);
                SwingUtilities.invokeLater(() -> mapLabel.setText(text("Karte konnte nicht geladen werden", "Map could not be loaded")));
            }
        }, "divera-dashboard-map-loader");
        mapThread.setDaemon(true);
        mapThread.start();
    }

    private void showAlarmDialog(LastAlarm alarm) {
        Color accent = alarm.isPriority() ? new Color(190, 40, 40) : new Color(20, 100, 180);
        JDialog dialog = new JDialog((Frame) null, valueOrFallback(alarm.getTitle(), text("Divera247 Einsatz", "Divera247 Alert")), true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(backgroundColor());
        root.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(backgroundColor());
        JLabel icon = new JLabel(alarm.isPriority() ? "!" : "i", SwingConstants.CENTER);
        icon.setOpaque(true);
        icon.setBackground(accent);
        icon.setForeground(Color.WHITE);
        icon.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 24));
        icon.setPreferredSize(new Dimension(42, 42));

        JLabel title = new JLabel(valueOrFallback(alarm.getTitle(), text("Neuer Einsatz", "New Alert")));
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        title.setForeground(textColor());

        JLabel priority = new JLabel(alarm.isPriority() ? text("PRIORITÄT", "PRIORITY") : "INFO");
        priority.setOpaque(true);
        priority.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 70));
        priority.setForeground(new Color(245, 248, 252));
        priority.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        priority.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel titleBlock = new JPanel(new BorderLayout(0, 6));
        titleBlock.setBackground(backgroundColor());
        titleBlock.add(title, BorderLayout.CENTER);
        titleBlock.add(priority, BorderLayout.SOUTH);
        header.add(icon, BorderLayout.WEST);
        header.add(titleBlock, BorderLayout.CENTER);

        JPanel details = new JPanel(new GridBagLayout());
        details.setBackground(surfaceColor());
        details.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(14, 14, 14, 14)
        ));

        addRow(details, 0, text("Meldung", "Message"), valueOrFallback(alarm.getText(), "-"));
        addRow(details, 1, text("Adresse", "Address"), valueOrFallback(alarm.getAddress(), "-"));
        addMap(details, 2, alarm);

        JButton okButton = new JButton("OK");
        okButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        okButton.setForeground(Color.WHITE);
        okButton.setBackground(accent);
        okButton.setFocusPainted(false);
        okButton.setBorder(BorderFactory.createEmptyBorder(9, 24, 9, 24));
        okButton.addActionListener(event -> dialog.dispose());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(backgroundColor());
        footer.add(okButton, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(details, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        dialog.setContentPane(root);
        dialog.setMinimumSize(new Dimension(460, 280));
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.setVisible(true);
    }

    private void addRow(JPanel panel, int row, String label, String value) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(row == 0 ? 0 : 12, 0, 0, 14);

        JLabel labelComponent = new JLabel(label);
        labelComponent.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        labelComponent.setForeground(mutedTextColor());
        panel.add(labelComponent, labelConstraints);

        JTextArea valueComponent = new JTextArea(value);
        valueComponent.setEditable(false);
        valueComponent.setLineWrap(true);
        valueComponent.setWrapStyleWord(true);
        valueComponent.setOpaque(false);
        valueComponent.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        valueComponent.setForeground(textColor());
        valueComponent.setCaretColor(textColor());

        JScrollPane scrollPane = new JScrollPane(valueComponent);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setPreferredSize(new Dimension(320, row == 0 ? 92 : 46));

        GridBagConstraints valueConstraints = new GridBagConstraints();
        valueConstraints.gridx = 1;
        valueConstraints.gridy = row;
        valueConstraints.weightx = 1.0;
        valueConstraints.fill = GridBagConstraints.HORIZONTAL;
        valueConstraints.insets = new Insets(row == 0 ? 0 : 12, 0, 0, 0);
        panel.add(scrollPane, valueConstraints);
    }

    private void addMap(JPanel panel, int row, LastAlarm alarm) {
        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.NORTHWEST;
        labelConstraints.insets = new Insets(12, 0, 0, 14);

        JLabel labelComponent = new JLabel(text("Karte", "Map"));
        labelComponent.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        labelComponent.setForeground(mutedTextColor());
        panel.add(labelComponent, labelConstraints);

        JLabel mapLabel = new JLabel(text("Karte wird geladen...", "Loading map..."), SwingConstants.CENTER);
        mapLabel.setOpaque(true);
        mapLabel.setBackground(surfaceAltColor());
        mapLabel.setForeground(mutedTextColor());
        mapLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        mapLabel.setPreferredSize(new Dimension(512, 256));
        mapLabel.setBorder(BorderFactory.createLineBorder(borderColor()));

        JButton zoomOutButton = new JButton("-");
        JButton zoomInButton = new JButton("+");
        styleMapButton(zoomOutButton);
        styleMapButton(zoomInButton);

        JPanel controls = new JPanel(new BorderLayout(6, 0));
        controls.setOpaque(false);
        controls.add(zoomOutButton, BorderLayout.WEST);
        controls.add(zoomInButton, BorderLayout.EAST);

        JPanel mapContainer = new JPanel(new BorderLayout(0, 6));
        mapContainer.setOpaque(false);
        mapContainer.add(controls, BorderLayout.NORTH);
        mapContainer.add(mapLabel, BorderLayout.CENTER);

        GridBagConstraints mapConstraints = new GridBagConstraints();
        mapConstraints.gridx = 1;
        mapConstraints.gridy = row;
        mapConstraints.weightx = 1.0;
        mapConstraints.fill = GridBagConstraints.HORIZONTAL;
        mapConstraints.insets = new Insets(12, 0, 0, 0);
        panel.add(mapContainer, mapConstraints);

        String address = valueOrFallback(alarm.getAddress(), "");
        if (isBlank(address)) {
            mapLabel.setText(text("Keine Adresse vorhanden", "No address available"));
            return;
        }

        MapView mapView = new MapView(mapLabel, zoomOutButton, zoomInButton);
        zoomOutButton.addActionListener(event -> mapView.zoomOut());
        zoomInButton.addActionListener(event -> mapView.zoomIn());

        Thread mapThread = new Thread(() -> loadMap(alarm, mapView), "divera-map-loader");
        mapThread.setDaemon(true);
        mapThread.start();
    }

    private void styleMapButton(JButton button) {
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        button.setFocusPainted(false);
        button.setForeground(textColor());
        button.setBackground(surfaceAltColor());
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor()),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
    }

    private void loadMap(LastAlarm alarm, MapView mapView) {
        try {
            Coordinates coordinates = findCoordinates(alarm);
            mapView.setCoordinates(coordinates);
        } catch (Exception exception) {
            log.warn("Failed to load map for address: {}", alarm.getAddress(), exception);
            SwingUtilities.invokeLater(() -> mapView.showError());
        }
    }

    private Coordinates findCoordinates(LastAlarm alarm) throws IOException, InterruptedException {
        Coordinates coordinates = parseCoordinates(alarm.getAddress());
        if (coordinates != null) {
            return coordinates;
        }

        coordinates = parseCoordinates(alarm.getText());
        if (coordinates != null) {
            return coordinates;
        }

        IOException lastException = null;
        for (String candidate : buildAddressCandidates(alarm)) {
            try {
                return geocode(candidate);
            } catch (IOException exception) {
                lastException = exception;
            }
        }

        throw lastException == null ? new IOException("No address candidates found") : lastException;
    }

    private Coordinates parseCoordinates(String value) {
        if (value == null) {
            return null;
        }

        Matcher matcher = COORDINATE_PATTERN.matcher(value);
        if (!matcher.find()) {
            return null;
        }

        double first = Double.parseDouble(matcher.group(1).replace(',', '.'));
        double second = Double.parseDouble(matcher.group(2).replace(',', '.'));
        if (Math.abs(first) <= 90.0 && Math.abs(second) <= 180.0) {
            return new Coordinates(first, second);
        }
        if (Math.abs(second) <= 90.0 && Math.abs(first) <= 180.0) {
            return new Coordinates(second, first);
        }
        return null;
    }

    private Set<String> buildAddressCandidates(LastAlarm alarm) {
        Set<String> candidates = new LinkedHashSet<>();
        addAddressCandidate(candidates, alarm.getAddress());
        addAddressCandidate(candidates, alarm.getText());
        addAddressCandidate(candidates, alarm.getTitle());
        return candidates;
    }

    private void addAddressCandidate(Set<String> candidates, String value) {
        if (isBlank(value)) {
            return;
        }

        String normalized = value
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
        normalized = normalized.replaceAll("\\([^)]*\\)", "").replaceAll("\\[[^]]*]", "").trim();
        if (normalized.length() < 5) {
            return;
        }

        candidates.add(normalized);
        candidates.add(normalized + ", Deutschland");

        String[] parts = normalized.split("[,;]");
        if (parts.length > 0 && parts[0].trim().length() >= 5) {
            candidates.add(parts[0].trim());
            candidates.add(parts[0].trim() + ", Deutschland");
        }
    }

    private Coordinates geocode(String address) throws IOException, InterruptedException {
        String query = URLEncoder.encode(address, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=de&accept-language=de&q=" + query))
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "divera247-poller/1.0")
                .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Geocoding failed with status " + response.statusCode());
        }
        Matcher latMatcher = LAT_PATTERN.matcher(response.body());
        Matcher lonMatcher = LON_PATTERN.matcher(response.body());
        if (!latMatcher.find() || !lonMatcher.find()) {
            throw new IOException("Address not found");
        }
        return new Coordinates(Double.parseDouble(latMatcher.group(1)), Double.parseDouble(lonMatcher.group(1)));
    }

    private BufferedImage renderMap(Coordinates coordinates, int zoom) throws IOException {
        int tileSize = 256;
        int width = 512;
        int height = 256;
        double scale = Math.pow(2, zoom);
        double centerX = (coordinates.lon + 180.0) / 360.0 * scale;
        double centerY = (1.0 - Math.log(Math.tan(Math.toRadians(coordinates.lat)) + 1.0 / Math.cos(Math.toRadians(coordinates.lat))) / Math.PI) / 2.0 * scale;
        int startTileX = (int) Math.floor(centerX - 1);
        int startTileY = (int) Math.floor(centerY - 0.5);

        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = map.createGraphics();
        graphics.setColor(new Color(235, 239, 244));
        graphics.fillRect(0, 0, width, height);

        for (int x = 0; x < 3; x++) {
            for (int y = 0; y < 2; y++) {
                int tileX = startTileX + x;
                int tileY = startTileY + y;
                BufferedImage tile = readTile(zoom, tileX, tileY);
                int drawX = (int) Math.round((tileX - centerX) * tileSize + width / 2.0);
                int drawY = (int) Math.round((tileY - centerY) * tileSize + height / 2.0);
                graphics.drawImage(tile, drawX, drawY, null);
            }
        }

        drawMarker(graphics, width / 2, height / 2);
        graphics.dispose();
        return map;
    }

    private BufferedImage renderDashboardMap(Coordinates coordinates, int zoom, int width, int height) throws IOException {
        int tileSize = 256;
        double scale = Math.pow(2, zoom);
        double centerX = (coordinates.lon + 180.0) / 360.0 * scale;
        double centerY = (1.0 - Math.log(Math.tan(Math.toRadians(coordinates.lat)) + 1.0 / Math.cos(Math.toRadians(coordinates.lat))) / Math.PI) / 2.0 * scale;
        int tileColumns = (int) Math.ceil(width / (double) tileSize) + 2;
        int tileRows = (int) Math.ceil(height / (double) tileSize) + 2;
        int startTileX = (int) Math.floor(centerX - tileColumns / 2.0);
        int startTileY = (int) Math.floor(centerY - tileRows / 2.0);

        BufferedImage map = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = map.createGraphics();
        graphics.setColor(new Color(2, 17, 29));
        graphics.fillRect(0, 0, width, height);

        for (int x = 0; x < tileColumns; x++) {
            for (int y = 0; y < tileRows; y++) {
                int tileX = startTileX + x;
                int tileY = startTileY + y;
                BufferedImage tile = readTile(zoom, tileX, tileY);
                int drawX = (int) Math.round((tileX - centerX) * tileSize + width / 2.0);
                int drawY = (int) Math.round((tileY - centerY) * tileSize + height / 2.0);
                graphics.drawImage(tile, drawX, drawY, null);
            }
        }

        graphics.setColor(new Color(0, 12, 24, 182));
        graphics.fillRect(0, 0, width, height);
        graphics.setPaint(new GradientPaint(width - 240, 0, new Color(2, 18, 32, 30), width, 0, new Color(2, 18, 32, 210)));
        graphics.fillRect(0, 0, width, height);
        drawDashboardMarker(graphics, width / 2, height / 2);
        graphics.dispose();
        return map;
    }

    private BufferedImage readTile(int zoom, int tileX, int tileY) throws IOException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://tile.openstreetmap.org/" + zoom + "/" + tileX + "/" + tileY + ".png"))
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "divera247-poller")
                    .build();
            HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
            return ImageIO.read(new ByteArrayInputStream(response.body()));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while loading map tile", exception);
        }
    }

    private void drawMarker(Graphics2D graphics, int x, int y) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setColor(new Color(190, 40, 40));
        graphics.fillOval(x - 9, y - 24, 18, 18);
        graphics.fillPolygon(new int[]{x - 6, x + 6, x}, new int[]{y - 10, y - 10, y + 4}, 3);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(x - 4, y - 19, 8, 8);
    }

    private void drawDashboardMarker(Graphics2D graphics, int x, int y) {
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setStroke(new BasicStroke(2f));
        for (int radius = 24; radius <= 58; radius += 16) {
            graphics.setColor(new Color(255, 72, 65, Math.max(35, 165 - radius * 2)));
            graphics.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        }
        graphics.setColor(new Color(255, 72, 65, 120));
        graphics.fillOval(x - 28, y - 28, 56, 56);
        graphics.setColor(new Color(255, 72, 65));
        graphics.fillOval(x - 11, y - 11, 22, 22);
        graphics.setColor(Color.WHITE);
        graphics.fillOval(x - 5, y - 5, 10, 10);
    }

    private String valueOrFallback(String value, String fallback) {
        if (isBlank(value)) {
            return fallback;
        }
        return value;
    }

    private String text(String german, String english) {
        return config.isGerman() ? german : english;
    }

    private Color backgroundColor() {
        return config.isDarkMode() ? DARK_BACKGROUND : LIGHT_BACKGROUND;
    }

    private Color surfaceColor() {
        return config.isDarkMode() ? DARK_SURFACE : LIGHT_SURFACE;
    }

    private Color surfaceAltColor() {
        return config.isDarkMode() ? DARK_SURFACE_ALT : LIGHT_SURFACE_ALT;
    }

    private Color borderColor() {
        return config.isDarkMode() ? DARK_BORDER : LIGHT_BORDER;
    }

    private Color textColor() {
        return config.isDarkMode() ? DARK_TEXT : LIGHT_TEXT;
    }

    private Color mutedTextColor() {
        return config.isDarkMode() ? DARK_MUTED_TEXT : LIGHT_MUTED_TEXT;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private class MapView {
        private final JLabel mapLabel;
        private final JButton zoomOutButton;
        private final JButton zoomInButton;
        private Coordinates coordinates;
        private int zoom = config.defaultMapZoom();

        private MapView(JLabel mapLabel, JButton zoomOutButton, JButton zoomInButton) {
            this.mapLabel = mapLabel;
            this.zoomOutButton = zoomOutButton;
            this.zoomInButton = zoomInButton;
            setButtonsEnabled(false);
        }

        private void setCoordinates(Coordinates coordinates) {
            this.coordinates = coordinates;
            setButtonsEnabled(true);
            reload();
        }

        private void zoomOut() {
            if (coordinates == null || zoom <= config.minMapZoom()) {
                return;
            }
            zoom--;
            reload();
        }

        private void zoomIn() {
            if (coordinates == null || zoom >= config.maxMapZoom()) {
                return;
            }
            zoom++;
            reload();
        }

        private void reload() {
            Coordinates currentCoordinates = coordinates;
            int currentZoom = zoom;
            SwingUtilities.invokeLater(() -> {
                mapLabel.setIcon(null);
                mapLabel.setText(text("Karte wird geladen...", "Loading map..."));
                updateButtons();
            });

            Thread renderThread = new Thread(() -> {
                try {
                    BufferedImage map = renderMap(currentCoordinates, currentZoom);
                    SwingUtilities.invokeLater(() -> {
                        mapLabel.setText(null);
                        mapLabel.setIcon(new ImageIcon(map));
                        updateButtons();
                    });
                } catch (Exception exception) {
                    log.warn("Failed to render map", exception);
                    SwingUtilities.invokeLater(() -> showError());
                }
            }, "divera-map-renderer");
            renderThread.setDaemon(true);
            renderThread.start();
        }

        private void showError() {
            mapLabel.setIcon(null);
            mapLabel.setText(text("Karte konnte nicht geladen werden", "Map could not be loaded"));
            setButtonsEnabled(false);
        }

        private void updateButtons() {
            zoomOutButton.setEnabled(coordinates != null && zoom > config.minMapZoom());
            zoomInButton.setEnabled(coordinates != null && zoom < config.maxMapZoom());
        }

        private void setButtonsEnabled(boolean enabled) {
            zoomOutButton.setEnabled(enabled);
            zoomInButton.setEnabled(enabled);
        }
    }

    private static class Coordinates {
        private final double lat;
        private final double lon;

        private Coordinates(double lat, double lon) {
            this.lat = lat;
            this.lon = lon;
        }
    }

    private static class RoundedPanel extends JPanel {
        private final Color fillColor;
        private final Color strokeColor;
        private final int radius;

        private RoundedPanel(java.awt.LayoutManager layout, Color fillColor, Color strokeColor, int radius) {
            super(layout);
            this.fillColor = fillColor;
            this.strokeColor = strokeColor;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(fillColor);
            graphics2D.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            graphics2D.dispose();
            super.paintComponent(graphics);
        }

        @Override
        protected void paintBorder(Graphics graphics) {
            Graphics2D graphics2D = (Graphics2D) graphics.create();
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setColor(strokeColor);
            graphics2D.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            graphics2D.dispose();
        }
    }
}
