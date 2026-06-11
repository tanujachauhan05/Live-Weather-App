import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;
import java.util.List;
import org.json.JSONObject;

public class InteractiveWeatherApp extends JFrame {
    // --- API KEY HERE ---
    private static final String API_KEY = "03e340d7e1271c0002ed0f45403f85c6";
    private static final String API_URL = "https://api.openweathermap.org/data/2.5/weather";

    // Components
    private JTextField cityField;
    private AnimatedButton searchButton;
    private DynamicBackgroundPanel backgroundPanel;
    private JPanel mainContentPanel;

    // Data Labels
    private JLabel cityLabel, tempLabel, conditionLabel;
    private MetricCard humidityCard, windCard, pressureCard, feelsCard;

    public InteractiveWeatherApp() {
        setTitle("Weather Interactive");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 750);
        setLocationRelativeTo(null);

        // 1. Setup Dynamic Background
        backgroundPanel = new DynamicBackgroundPanel();
        backgroundPanel.setLayout(new BorderLayout());
        setContentPane(backgroundPanel);

        // 2. Initialize UI
        setupUI();

        // 3. Start Animation Loop (60 FPS)
        new Timer(16, e -> backgroundPanel.animate()).start();

        setVisible(true);
    }

    private void setupUI() {
        // --- Top Search Bar ---
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 20));
        topBar.setOpaque(false);

        cityField = new JTextField(15) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(new Color(30, 30, 40, 200));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        cityField.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        cityField.setForeground(Color.WHITE);
        cityField.setCaretColor(Color.CYAN);
        cityField.setBorder(new EmptyBorder(5, 15, 5, 15));
        cityField.setOpaque(false);

        // Glow effect on focus
        cityField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { cityField.repaint(); }
            public void focusLost(FocusEvent e) { cityField.repaint(); }
        });

        searchButton = new AnimatedButton("Search");
        searchButton.addActionListener(e -> fetchWeather());
        cityField.addActionListener(e -> fetchWeather());

        topBar.add(cityField);
        topBar.add(searchButton);

        // --- Center Weather Display ---
        mainContentPanel = new JPanel();
        mainContentPanel.setLayout(new BoxLayout(mainContentPanel, BoxLayout.Y_AXIS));
        mainContentPanel.setOpaque(false);

        cityLabel = createLabel("ENTER CITY", 32, Font.BOLD);
        tempLabel = createLabel("--", 80, Font.BOLD);
        tempLabel.setForeground(new Color(0, 255, 255)); // Cyan
        conditionLabel = createLabel("Ready", 24, Font.ITALIC);

        mainContentPanel.add(Box.createVerticalStrut(50));
        mainContentPanel.add(cityLabel);
        mainContentPanel.add(tempLabel);
        mainContentPanel.add(conditionLabel);
        mainContentPanel.add(Box.createVerticalStrut(50));

        // --- Bottom Metric Cards (Interactive) ---
        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setBorder(new EmptyBorder(20, 40, 50, 40));

        humidityCard = new MetricCard("Humidity", "%");
        windCard = new MetricCard("Wind", "m/s");
        pressureCard = new MetricCard("Pressure", "hPa");
        feelsCard = new MetricCard("Feels Like", "°C");

        cardsPanel.add(feelsCard);
        cardsPanel.add(humidityCard);
        cardsPanel.add(windCard);
        cardsPanel.add(pressureCard);

        backgroundPanel.add(topBar, BorderLayout.NORTH);
        backgroundPanel.add(mainContentPanel, BorderLayout.CENTER);
        backgroundPanel.add(cardsPanel, BorderLayout.SOUTH);
    }

    private JLabel createLabel(String text, int size, int style) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(Color.WHITE);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        return l;
    }

    // --- LOGIC ---
    private void fetchWeather() {
        String city = cityField.getText().trim();
        if(city.isEmpty()) return;

        searchButton.setText("...");
        searchButton.setEnabled(false);

        new SwingWorker<JSONObject, Void>() {
            @Override
            protected JSONObject doInBackground() throws Exception {
                URL url = new URL(API_URL + "?q=" + city + "&appid=" + API_KEY + "&units=metric");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = reader.readLine()) != null) sb.append(line);
                return new JSONObject(sb.toString());
            }

            @Override
            protected void done() {
                searchButton.setText("Search");
                searchButton.setEnabled(true);
                try {
                    JSONObject json = get();
                    updateData(json);
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(null, "City not found!");
                }
            }
        }.execute();
    }

    private void updateData(JSONObject json) {
        String city = json.getString("name");
        JSONObject main = json.getJSONObject("main");
        JSONObject wind = json.getJSONObject("wind");
        JSONObject weather = json.getJSONArray("weather").getJSONObject(0);
        String desc = weather.getString("main"); // Rain, Clear, Clouds

        // Text Updates
        cityLabel.setText(city.toUpperCase());
        double temp = main.getDouble("temp");
        tempLabel.setText(String.format("%.0f°", temp));
        conditionLabel.setText(desc.toUpperCase());

        // Card Updates
        feelsCard.setValue(String.format("%.1f", main.getDouble("feels_like")));
        humidityCard.setValue(String.valueOf(main.getInt("humidity")));
        windCard.setValue(String.format("%.1f", wind.getDouble("speed")));
        pressureCard.setValue(String.valueOf(main.getInt("pressure")));

        // Trigger Background Animation Change
        backgroundPanel.setWeatherCondition(desc);
    }

    // --- CUSTOM INTERACTIVE COMPONENTS ---

    // 1. Metric Card that "Lifts" on Hover
    class MetricCard extends JPanel {
        private String title, unit, value = "--";
        private boolean isHovered = false;
        private int hoverOffset = 0;

        public MetricCard(String title, String unit) {
            this.title = title;
            this.unit = unit;
            setOpaque(false);
            setPreferredSize(new Dimension(150, 120));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { isHovered = true; repaint(); }
                public void mouseExited(MouseEvent e) { isHovered = false; repaint(); }
            });
        }

        public void setValue(String v) { this.value = v; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Animation Logic for "Lift"
            if (isHovered && hoverOffset < 10) hoverOffset += 2;
            else if (!isHovered && hoverOffset > 0) hoverOffset -= 2;

            int yPos = 10 - hoverOffset; // Moves up

            // Background Gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(255, 255, 255, 30), 0, getHeight(), new Color(255, 255, 255, 10));
            g2d.setPaint(gp);
            g2d.fillRoundRect(0, yPos, getWidth(), getHeight()-20, 20, 20);

            // Border Glow
            if (isHovered) {
                g2d.setColor(new Color(0, 255, 255, 100));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, yPos, getWidth()-1, getHeight()-21, 20, 20);
            }

            // Text
            g2d.setColor(new Color(200, 200, 200));
            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2d.drawString(title.toUpperCase(), 15, yPos + 25);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
            g2d.drawString(value, 15, yPos + 60);

            g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            g2d.drawString(unit, getWidth() - 40, yPos + 60);
        }
    }

    // 2. Animated Background System
    class DynamicBackgroundPanel extends JPanel {
        private String weatherType = "Clear";
        private List<Point2D.Float> particles = new ArrayList<>();
        private Random random = new Random();
        private float sunRotation = 0;

        public DynamicBackgroundPanel() {
            // Init particles
            for(int i=0; i<100; i++) particles.add(new Point2D.Float(random.nextInt(1000), random.nextInt(800)));
        }

        public void setWeatherCondition(String type) {
            this.weatherType = type;
            repaint();
        }

        public void animate() {
            sunRotation += 0.5;

            for(Point2D.Float p : particles) {
                if (weatherType.toLowerCase().contains("rain")) {
                    p.y += 15; // Rain falls fast
                    p.x -= 2;  // Wind
                } else if (weatherType.toLowerCase().contains("cloud")) {
                    p.x -= 0.5; // Clouds move slow
                }

                // Reset positions
                if (p.y > getHeight()) p.y = -10;
                if (p.x < -50) p.x = getWidth() + 50;
            }
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Base Gradient (Dark Navy to Purple)
            GradientPaint bg = new GradientPaint(0, 0, new Color(15, 20, 40), 0, getHeight(), new Color(30, 20, 60));
            g2d.setPaint(bg);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            // Draw Weather Effects
            if (weatherType.toLowerCase().contains("clear")) {
                drawSun(g2d);
            } else if (weatherType.toLowerCase().contains("rain")) {
                drawRain(g2d);
            } else if (weatherType.toLowerCase().contains("cloud")) {
                drawClouds(g2d);
            }
        }

        private void drawSun(Graphics2D g2d) {
            g2d.setColor(new Color(255, 200, 50, 100)); // Yellow glow
            int cx = getWidth() / 2;
            int cy = 200;

            AffineTransform old = g2d.getTransform();
            g2d.rotate(Math.toRadians(sunRotation), cx, cy);

            // Draw rays
            for (int i = 0; i < 12; i++) {
                g2d.fillRoundRect(cx - 5, cy - 150, 10, 60, 10, 10);
                g2d.rotate(Math.toRadians(30), cx, cy);
            }
            g2d.setTransform(old);

            // Sun Core
            g2d.setColor(new Color(255, 180, 0));
            g2d.fillOval(cx - 60, cy - 60, 120, 120);
        }

        private void drawRain(Graphics2D g2d) {
            g2d.setColor(new Color(100, 200, 255, 150));
            g2d.setStroke(new BasicStroke(2));
            for(Point2D.Float p : particles) {
                g2d.drawLine((int)p.x, (int)p.y, (int)p.x - 5, (int)p.y + 15);
            }
        }

        private void drawClouds(Graphics2D g2d) {
            g2d.setColor(new Color(255, 255, 255, 20)); // Faint white
            for(Point2D.Float p : particles) {
                // Draw big soft blobs
                g2d.fillOval((int)p.x, (int)p.y % 300, 150, 80);
            }
        }
    }

    // 3. Modern Button
    class AnimatedButton extends JButton {
        public AnimatedButton(String text) {
            super(text);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setForeground(Color.WHITE);
            setFont(new Font("Segoe UI", Font.BOLD, 14));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if(getModel().isRollover()) g2d.setColor(new Color(0, 200, 255));
            else g2d.setColor(new Color(0, 150, 255));

            g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
            super.paintComponent(g);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InteractiveWeatherApp());
    }
}