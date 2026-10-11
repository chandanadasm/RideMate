package ui;

import exception.InvalidLoginException;
import exception.InvalidRideException;
import exception.RideNotFoundException;
import model.Driver;
import model.Ride;
import model.RideRequest;
import model.Student;
import model.Vehicle;
import service.AuthService;
import service.FileService;
import service.RideService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Presentation Layer: Modern Java Swing Graphical User Interface for RouteMate / CampusRide.
 *
 * Design System:
 * - Forest Green:  #163D32 (Primary branding & primary buttons)
 * - Deep Green:    #102F27 (Sidebar background & gradient dark stop)
 * - Rich Forest:   #1A473A (Gradient light stop)
 * - Warm Cream:    #F7F3E8 (Main dashboard backdrop)
 * - Ivory:         #FFFDF7 (Elevated rounded card panels)
 * - Sage Green:    #A8BDA5 (Secondary accents, borders & badges)
 * - Sage Button:   #CFE0CC (High-contrast secondary button background)
 * - Deep Forest:   #20352D (High-contrast readable body text)
 * - Muted Slate:   #4A5850 (Captions and secondary labels)
 * - Subtle Border: #E2E5D9 (Clean card and field outlines)
 *
 * Architecture:
 * - Swing UI handles presentation, layout, and user interactions.
 * - Does NOT contain business logic directly in event handlers.
 * - Delegates all business logic strictly to AuthService and RideService.
 * - CardLayout navigation manages all 8 application screens with unified navigation.
 */
public class CampusRideUI {

    // Screen Navigation Identifiers
    public static final String SCREEN_LOGIN = "SCREEN_LOGIN";
    public static final String SCREEN_REGISTER = "SCREEN_REGISTER";
    public static final String SCREEN_VERIFY = "SCREEN_VERIFY";
    public static final String SCREEN_DASHBOARD = "SCREEN_DASHBOARD";
    public static final String SCREEN_OFFER_RIDE = "SCREEN_OFFER_RIDE";
    public static final String SCREEN_SEARCH_RIDE = "SCREEN_SEARCH_RIDE";
    public static final String SCREEN_MY_RIDES = "SCREEN_MY_RIDES";
    public static final String SCREEN_PROFILE = "SCREEN_PROFILE";

    // RouteMate / CampusRide Theme Color Palette
    public static final Color COLOR_FOREST_GREEN = new Color(0x16, 0x3D, 0x32); // #163D32 Primary Dark Green
    public static final Color COLOR_DEEP_GREEN   = new Color(0x10, 0x2F, 0x27); // #102F27 Deep Green
    public static final Color COLOR_RICH_FOREST  = new Color(0x1A, 0x47, 0x3A); // #1A473A Gradient Light Stop
    public static final Color COLOR_CREAM        = new Color(0xF7, 0xF3, 0xE8); // #F7F3E8 Warm Cream Background
    public static final Color COLOR_IVORY_CARD   = new Color(0xFF, 0xFD, 0xF7); // #FFFDF7 Ivory Card Surface
    public static final Color COLOR_SAGE_GREEN   = new Color(0xA8, 0xBD, 0xA5); // #A8BDA5 Sage Green Accent
    public static final Color COLOR_SAGE_ACCENT  = new Color(0xCF, 0xE0, 0xCC); // #CFE0CC Sage Button Background
    public static final Color COLOR_MAIN_TEXT    = new Color(0x20, 0x35, 0x2D); // #20352D High-contrast Deep Forest Text
    public static final Color COLOR_MUTED_TEXT   = new Color(0x4A, 0x58, 0x50); // #4A5850 High-contrast Muted Text
    public static final Color COLOR_BORDER       = new Color(0xE2, 0xE5, 0xD9); // #E2E5D9 Subtle Warm Border

    // Extended UI State Colors
    public static final Color COLOR_HOVER_GREEN    = new Color(0x1F, 0x52, 0x44); // Primary button hover state
    public static final Color COLOR_SIDEBAR_ACTIVE = new Color(0x1D, 0x4A, 0x3E); // Active navigation item
    public static final Color COLOR_SIDEBAR_HOVER  = new Color(0x16, 0x3D, 0x32); // Inactive navigation hover
    public static final Color COLOR_DANGER         = new Color(0x99, 0x1B, 0x1B); // High-contrast Crimson Red
    public static final Color COLOR_DANGER_HOVER   = new Color(0xB9, 0x1C, 0x1C); // Danger hover
    public static final Color COLOR_SUCCESS        = new Color(0x15, 0x80, 0x3D); // Success green
    public static final Color COLOR_SUCCESS_BG     = new Color(0xF0, 0xFD, 0xF4); // Success badge background
    public static final Color COLOR_WARNING        = new Color(0xB4, 0x53, 0x09); // Warning amber
    public static final Color COLOR_WARNING_BG     = new Color(0xFF, 0xFB, 0xEB); // Warning badge background

    // Application Services
    private final AuthService authService;
    private final RideService rideService;
    private final FileService fileService;

    // Core Swing UI Components
    private JFrame mainFrame;
    private JPanel mainContainer;
    private CardLayout cardLayout;

    // Track email currently undergoing verification
    private String pendingVerificationEmail = "";
    private JTextField loginEmailField;

    // Dashboard dynamic labels & summary metrics
    private JLabel dashboardGreetingLabel;
    private DefaultTableModel dashboardRidesModel;
    private JLabel statAvailableCountLabel;
    private JLabel statMyRidesCountLabel;
    private JLabel statPendingRequestsLabel;
    private JLabel statVerificationBadgeLabel;

    // Search & My Rides Table Models
    private DefaultTableModel searchResultsModel;
    private DefaultTableModel myRidesModel;
    private DefaultTableModel incomingRequestsModel;
    private DefaultTableModel mySubmittedRequestsModel;

    // Profile Screen dynamic labels
    private JLabel profileNameLabel;
    private JLabel profileIdLabel;
    private JLabel profileEmailLabel;
    private JLabel profileRoleLabel;
    private JLabel profileVerifyStatusLabel;

    // Verification screen dynamic label
    private JLabel verifyTargetEmailLabel;
    private JLabel verifyStatusBadgeLabel;

    // Synchronized sidebar user labels across screens
    private final List<JLabel> sidebarUserLabels = new ArrayList<>();
    private final List<JLabel> sidebarEmailLabels = new ArrayList<>();

    public CampusRideUI() {
        this.authService = new AuthService();
        this.rideService = new RideService();
        this.fileService = new FileService();

        // Load academic file handling records into memory cache
        this.authService.setStudents(fileService.loadStudents());
        this.rideService.setRides(fileService.loadRides());

        // Sync real rides from Supabase if configured
        if (supabase.SupabaseConfig.isConfigured()) {
            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() {
                    rideService.syncRidesFromSupabase();
                    return null;
                }
            }.execute();
        }
    }

    /**
     * Initializes and displays the Swing GUI on the Event Dispatch Thread.
     */
    public void start() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }

            initFrame();
            showScreen(SCREEN_LOGIN);
            mainFrame.setVisible(true);
        });
    }

    /**
     * Configures the main application window and CardLayout card deck.
     */
    private void initFrame() {
        mainFrame = new JFrame("RouteMate - NSSCE College Ride Sharing System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1040, 720);
        mainFrame.setMinimumSize(new Dimension(920, 640));
        mainFrame.setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);
        mainContainer.setBackground(COLOR_CREAM);

        // Register each required screen panel
        mainContainer.add(createLoginPanel(), SCREEN_LOGIN);
        mainContainer.add(createRegisterPanel(), SCREEN_REGISTER);
        mainContainer.add(createVerifyPanel(), SCREEN_VERIFY);
        mainContainer.add(createDashboardPanel(), SCREEN_DASHBOARD);
        mainContainer.add(createOfferRidePanel(), SCREEN_OFFER_RIDE);
        mainContainer.add(createSearchRidePanel(), SCREEN_SEARCH_RIDE);
        mainContainer.add(createMyRidesPanel(), SCREEN_MY_RIDES);
        mainContainer.add(createProfilePanel(), SCREEN_PROFILE);

        mainFrame.setContentPane(mainContainer);
    }

    public void showScreen(String screenName) {
        updateSidebarUserInfo();
        if (SCREEN_DASHBOARD.equals(screenName)) {
            refreshDashboard();
        } else if (SCREEN_MY_RIDES.equals(screenName)) {
            refreshMyRides();
        } else if (SCREEN_PROFILE.equals(screenName)) {
            refreshProfile();
        } else if (SCREEN_VERIFY.equals(screenName)) {
            refreshVerifyScreen();
        }
        cardLayout.show(mainContainer, screenName);
    }

    /**
     * Keeps all sidebar student names and email labels updated with the active session.
     */
    private void updateSidebarUserInfo() {
        Student current = authService.getCurrentLoggedInStudent();
        String name = (current != null) ? current.getName() : "Student";
        String email = (current != null) ? current.getEmail() : "@nssce.ac.in";
        for (JLabel lbl : sidebarUserLabels) {
            lbl.setText(name);
        }
        for (JLabel lbl : sidebarEmailLabels) {
            lbl.setText(email);
        }
    }

    /**
     * Creates a subtle forest-green ambient gradient background for auth screens.
     */
    private JPanel createGradientBackdropPanel() {
        return new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Deep green to rich forest gradient
                GradientPaint gp = new GradientPaint(
                        0, 0, COLOR_DEEP_GREEN,
                        0, getHeight(), COLOR_RICH_FOREST
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Soft ambient sage aura behind the centered card
                g2.setColor(new Color(0xA8, 0xBD, 0xA5, 22));
                int cx = getWidth() / 2;
                int cy = getHeight() / 2;
                g2.fillOval(cx - 320, cy - 320, 640, 640);
                g2.dispose();
            }
        };
    }

    // =========================================================================
    // SIDEBAR & AUTHENTICATED SCREEN LAYOUT HELPER
    // =========================================================================
    private JPanel createAuthenticatedLayout(String activeScreen, JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_CREAM);
        wrapper.add(createSidebar(activeScreen), BorderLayout.WEST);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSidebar(String activeScreen) {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(230, 0));
        sidebar.setBackground(COLOR_DEEP_GREEN);
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x1B, 0x45, 0x39)));

        // Top Brand Header
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(26, 20, 20, 20));

        JLabel logoBadge = new JLabel("RouteMate");
        logoBadge.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logoBadge.setForeground(COLOR_IVORY_CARD);
        logoBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logoSubtitle = new JLabel("Campus Rides • NSSCE");
        logoSubtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        logoSubtitle.setForeground(COLOR_SAGE_GREEN);
        logoSubtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        brandPanel.add(logoBadge);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(logoSubtitle);

        // Navigation Menu Panel
        JPanel menuPanel = new JPanel();
        menuPanel.setLayout(new BoxLayout(menuPanel, BoxLayout.Y_AXIS));
        menuPanel.setOpaque(false);
        menuPanel.setBorder(new EmptyBorder(10, 12, 10, 12));

        menuPanel.add(createSidebarNavButton("Dashboard", SCREEN_DASHBOARD, activeScreen));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(createSidebarNavButton("Find Rides", SCREEN_SEARCH_RIDE, activeScreen));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(createSidebarNavButton("Offer a Ride", SCREEN_OFFER_RIDE, activeScreen));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(createSidebarNavButton("My Rides", SCREEN_MY_RIDES, activeScreen));
        menuPanel.add(Box.createVerticalStrut(6));
        menuPanel.add(createSidebarNavButton("Profile", SCREEN_PROFILE, activeScreen));
        menuPanel.add(Box.createVerticalStrut(6));
        JButton fuelNavBtn = createSidebarNavButton("Fuel Estimator", "ACTION_FUEL", activeScreen);
        for (java.awt.event.ActionListener al : fuelNavBtn.getActionListeners()) {
            fuelNavBtn.removeActionListener(al);
        }
        fuelNavBtn.addActionListener(e -> showPetrolCostEstimatorDialog(null));
        menuPanel.add(fuelNavBtn);

        // Bottom User Info & Logout
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.Y_AXIS));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(new EmptyBorder(16, 16, 20, 16));

        JPanel userCard = new JPanel(new BorderLayout(8, 2));
        userCard.setOpaque(false);
        userCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0x1B, 0x45, 0x39)),
                new EmptyBorder(12, 4, 12, 4)
        ));

        JLabel userNameLbl = new JLabel("Student");
        userNameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userNameLbl.setForeground(COLOR_IVORY_CARD);
        sidebarUserLabels.add(userNameLbl);

        JLabel userEmailLbl = new JLabel("@nssce.ac.in");
        userEmailLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userEmailLbl.setForeground(COLOR_SAGE_GREEN);
        sidebarEmailLabels.add(userEmailLbl);

        JPanel userTextPanel = new JPanel();
        userTextPanel.setLayout(new BoxLayout(userTextPanel, BoxLayout.Y_AXIS));
        userTextPanel.setOpaque(false);
        userTextPanel.add(userNameLbl);
        userTextPanel.add(Box.createVerticalStrut(2));
        userTextPanel.add(userEmailLbl);

        userCard.add(userTextPanel, BorderLayout.CENTER);

        JButton logoutBtn = createDangerButton("Logout");
        logoutBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        logoutBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutBtn.addActionListener(e -> handleLogout());

        bottomPanel.add(userCard);
        bottomPanel.add(Box.createVerticalStrut(8));
        bottomPanel.add(logoutBtn);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(menuPanel, BorderLayout.CENTER);
        sidebar.add(bottomPanel, BorderLayout.SOUTH);

        return sidebar;
    }

    private JButton createSidebarNavButton(String title, String targetScreen, String activeScreen) {
        boolean isActive = targetScreen.equals(activeScreen);
        JButton btn = new JButton(title) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", isActive ? Font.BOLD : Font.PLAIN, 13));
        btn.setForeground(isActive ? COLOR_IVORY_CARD : new Color(0xD0, 0xDC, 0xCE));
        btn.setBackground(isActive ? COLOR_SIDEBAR_ACTIVE : COLOR_DEEP_GREEN);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        btn.setPreferredSize(new Dimension(200, 40));

        if (isActive) {
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 4, 0, 0, COLOR_SAGE_GREEN),
                    new EmptyBorder(8, 14, 8, 14)
            ));
        } else {
            btn.setBorder(new EmptyBorder(8, 18, 8, 14));
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(COLOR_SIDEBAR_HOVER);
                    btn.setForeground(COLOR_IVORY_CARD);
                    btn.repaint();
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(COLOR_DEEP_GREEN);
                    btn.setForeground(new Color(0xD0, 0xDC, 0xCE));
                    btn.repaint();
                }
            });
        }

        btn.addActionListener(e -> showScreen(targetScreen));
        return btn;
    }

    // =========================================================================
    // SCREEN 1: Welcome & Login Screen (Premium Gradient Backdrop & Ivory Card)
    // =========================================================================
    private JPanel createLoginPanel() {
        // Subtle deep forest gradient background that complements the dashboard
        JPanel panel = createGradientBackdropPanel();

        // Elegant centered Ivory form card
        JPanel card = createCardPanel(450, 540);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        // RouteMate Brand Pill Badge
        JLabel tagBadge = new JLabel("🌱  NSSCE RIDE SHARING");
        tagBadge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        tagBadge.setForeground(COLOR_FOREST_GREEN);
        tagBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel brandBadge = new JLabel("RouteMate");
        brandBadge.setFont(new Font("Segoe UI", Font.BOLD, 30));
        brandBadge.setForeground(COLOR_FOREST_GREEN);
        brandBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Campus Rides, Made Easier");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(COLOR_MAIN_TEXT);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Safe, convenient & sustainable college travel");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(COLOR_MUTED_TEXT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel emailLabel = new JLabel("College Email (@nssce.ac.in):");
        emailLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        emailLabel.setForeground(COLOR_MAIN_TEXT);
        emailLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginEmailField = createStyledTextField();
        loginEmailField.setMaximumSize(new Dimension(390, 38));

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLabel.setForeground(COLOR_MAIN_TEXT);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPasswordField passField = createStyledPasswordField();
        passField.setMaximumSize(new Dimension(390, 38));

        // High-contrast, explicitly painted buttons
        JButton loginButton = createPrimaryButton("Login to Account");
        loginButton.setMaximumSize(new Dimension(390, 42));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton registerButton = createSecondaryButton("Create New Account");
        registerButton.setMaximumSize(new Dimension(390, 38));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton verifyStatusButton = createOutlineButton("Check Email Verification Status");
        verifyStatusButton.setMaximumSize(new Dimension(390, 36));
        verifyStatusButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Event: Login Action (Delegates strictly to AuthService)
        loginButton.addActionListener(e -> {
            String email = loginEmailField.getText().trim();
            String password = new String(passField.getPassword()).trim();

            if (email.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Please enter both college email and password.", "Input Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!email.toLowerCase().endsWith("@nssce.ac.in")) {
                JOptionPane.showMessageDialog(mainFrame, "Only @nssce.ac.in college email addresses are allowed.", "Login Rejected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            loginButton.setEnabled(false);
            loginButton.setText("Logging in...");

            SwingWorker<Student, Void> loginWorker = new SwingWorker<>() {
                @Override
                protected Student doInBackground() throws Exception {
                    return authService.login(email, password);
                }

                @Override
                protected void done() {
                    loginButton.setEnabled(true);
                    loginButton.setText("Login to Account");
                    try {
                        Student loggedIn = get();
                        if (loggedIn != null) {
                            passField.setText("");
                            showScreen(SCREEN_DASHBOARD);
                        }
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        String msg = cause.getMessage() != null ? cause.getMessage() : "Login failed";
                        if (msg.contains("verify your college email")) {
                            pendingVerificationEmail = email;
                            int opt = JOptionPane.showConfirmDialog(mainFrame,
                                    msg + "\n\nWould you like to open the Email Verification screen now?",
                                    "Email Verification Required", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                            if (opt == JOptionPane.YES_OPTION) {
                                showScreen(SCREEN_VERIFY);
                            }
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, msg, "Login Failed", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            };
            loginWorker.execute();
        });

        registerButton.addActionListener(e -> showScreen(SCREEN_REGISTER));

        verifyStatusButton.addActionListener(e -> {
            String email = loginEmailField.getText().trim();
            if (!email.isEmpty()) {
                pendingVerificationEmail = email;
            }
            showScreen(SCREEN_VERIFY);
        });

        // Assemble Card
        card.add(Box.createVerticalStrut(6));
        card.add(tagBadge);
        card.add(Box.createVerticalStrut(4));
        card.add(brandBadge);
        card.add(Box.createVerticalStrut(4));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(2));
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(22));
        card.add(emailLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(loginEmailField);
        card.add(Box.createVerticalStrut(14));
        card.add(passLabel);
        card.add(Box.createVerticalStrut(5));
        card.add(passField);
        card.add(Box.createVerticalStrut(22));
        card.add(loginButton);
        card.add(Box.createVerticalStrut(10));
        card.add(registerButton);
        card.add(Box.createVerticalStrut(8));
        card.add(verifyStatusButton);
        card.add(Box.createVerticalStrut(10));

        panel.add(card);
        return panel;
    }

    // =========================================================================
    // SCREEN 2: Registration Screen
    // =========================================================================
    private JPanel createRegisterPanel() {
        JPanel panel = createGradientBackdropPanel();

        JPanel card = createCardPanel(490, 640);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Create NSSCE Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_FOREST_GREEN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel noticeLabel = new JLabel("Official domain: only @nssce.ac.in emails accepted");
        noticeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        noticeLabel.setForeground(COLOR_SUCCESS);
        noticeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField nameField = createStyledTextField();
        JTextField regNumField = createStyledTextField();
        JTextField emailField = createStyledTextField();
        JPasswordField passField = createStyledPasswordField();
        JPasswordField confirmPassField = createStyledPasswordField();

        JButton registerBtn = createPrimaryButton("Register Account");
        registerBtn.setMaximumSize(new Dimension(420, 40));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton backBtn = createSecondaryButton("← Back to Login");
        backBtn.setMaximumSize(new Dimension(420, 36));
        backBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String regNum = regNumField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passField.getPassword()).trim();
            String confirmPassword = new String(confirmPassField.getPassword()).trim();

            if (name.isEmpty() || regNum.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "All fields are required. Please fill in all fields.", "Missing Information", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!email.toLowerCase().endsWith("@nssce.ac.in")) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Only @nssce.ac.in college email addresses are allowed.",
                        "Registration Rejected", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Passwords do not match. Please re-enter your password.",
                        "Password Mismatch", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (password.length() < 6) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Password must be at least 6 characters long.",
                        "Weak Password", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Student newStudent = new Student(name, regNum, email, password);

            registerBtn.setEnabled(false);
            registerBtn.setText("Registering with Supabase...");

            SwingWorker<Boolean, Void> registerWorker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    return authService.registerStudent(newStudent);
                }

                @Override
                protected void done() {
                    registerBtn.setEnabled(true);
                    registerBtn.setText("Register Account");
                    try {
                        boolean success = get();
                        if (success) {
                            fileService.saveStudent(newStudent);
                            pendingVerificationEmail = email;
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Registration successful. Please verify your college email before logging in.",
                                    "Registration Successful", JOptionPane.INFORMATION_MESSAGE);

                            nameField.setText("");
                            regNumField.setText("");
                            emailField.setText("");
                            passField.setText("");
                            confirmPassField.setText("");

                            if (loginEmailField != null) {
                                loginEmailField.setText(email);
                            }
                            showScreen(SCREEN_LOGIN);
                        }
                    } catch (Exception ex) {
                        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                        JOptionPane.showMessageDialog(mainFrame, cause.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            registerWorker.execute();
        });

        backBtn.addActionListener(e -> showScreen(SCREEN_LOGIN));

        card.add(Box.createVerticalStrut(10));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(noticeLabel);
        card.add(Box.createVerticalStrut(16));

        addFormField(card, "Full Name:", nameField);
        addFormField(card, "Registration Number:", regNumField);
        addFormField(card, "College Email (@nssce.ac.in):", emailField);
        addFormField(card, "Password:", passField);
        addFormField(card, "Confirm Password:", confirmPassField);

        card.add(Box.createVerticalStrut(10));
        card.add(registerBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(backBtn);
        card.add(Box.createVerticalStrut(10));

        panel.add(card);
        return panel;
    }

    // =========================================================================
    // SCREEN 3: Email Verification Status Screen
    // =========================================================================
    private JPanel createVerifyPanel() {
        JPanel panel = createGradientBackdropPanel();

        JPanel card = createCardPanel(500, 500);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("College Email Verification");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(COLOR_FOREST_GREEN);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("NSSCE Student Account Security");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(COLOR_MUTED_TEXT);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        verifyTargetEmailLabel = new JLabel("Email: None selected");
        verifyTargetEmailLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        verifyTargetEmailLabel.setForeground(COLOR_MAIN_TEXT);
        verifyTargetEmailLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        verifyStatusBadgeLabel = new JLabel("STATUS: PENDING VERIFICATION ⏳");
        verifyStatusBadgeLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        verifyStatusBadgeLabel.setForeground(COLOR_WARNING);
        verifyStatusBadgeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextArea infoText = new JTextArea("A confirmation link was dispatched to your @nssce.ac.in inbox via Supabase Auth.\n\n"
                + "Students must confirm their college email address before accessing the CampusRide dashboard.");
        infoText.setWrapStyleWord(true);
        infoText.setLineWrap(true);
        infoText.setOpaque(false);
        infoText.setEditable(false);
        infoText.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        infoText.setForeground(COLOR_MUTED_TEXT);
        infoText.setMaximumSize(new Dimension(420, 70));
        infoText.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton checkStatusBtn = createPrimaryButton("Check Supabase Verification Status");
        checkStatusBtn.setMaximumSize(new Dimension(420, 38));
        checkStatusBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton simulateVerifyBtn = createSecondaryButton("Simulate Email Confirmation (Dev/Demo)");
        simulateVerifyBtn.setMaximumSize(new Dimension(420, 36));
        simulateVerifyBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton toLoginBtn = createOutlineButton("← Go to Login Screen");
        toLoginBtn.setMaximumSize(new Dimension(420, 36));
        toLoginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        checkStatusBtn.addActionListener(e -> {
            if (pendingVerificationEmail.isEmpty()) {
                String input = JOptionPane.showInputDialog(mainFrame, "Enter @nssce.ac.in email to check:", "Check Email", JOptionPane.QUESTION_MESSAGE);
                if (input != null && !input.trim().isEmpty()) {
                    pendingVerificationEmail = input.trim();
                }
            }

            if (!pendingVerificationEmail.isEmpty()) {
                boolean verified = authService.isEmailVerified(pendingVerificationEmail);
                if (verified) {
                    verifyStatusBadgeLabel.setText("STATUS: VERIFIED ✔");
                    verifyStatusBadgeLabel.setForeground(COLOR_SUCCESS);
                    JOptionPane.showMessageDialog(mainFrame, "Your email is verified! You can now log in.", "Email Verified", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    verifyStatusBadgeLabel.setText("STATUS: PENDING VERIFICATION ⏳");
                    verifyStatusBadgeLabel.setForeground(COLOR_WARNING);
                    JOptionPane.showMessageDialog(mainFrame, "Verification is still pending. Please click the link sent to your college email.", "Pending", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        simulateVerifyBtn.addActionListener(e -> {
            if (pendingVerificationEmail.isEmpty()) {
                String input = JOptionPane.showInputDialog(mainFrame, "Enter @nssce.ac.in email to verify:", "Verify Email", JOptionPane.QUESTION_MESSAGE);
                if (input != null && !input.trim().isEmpty()) {
                    pendingVerificationEmail = input.trim();
                }
            }

            if (!pendingVerificationEmail.isEmpty()) {
                authService.markEmailAsVerified(pendingVerificationEmail);
                verifyStatusBadgeLabel.setText("STATUS: VERIFIED ✔");
                verifyStatusBadgeLabel.setForeground(COLOR_SUCCESS);
                JOptionPane.showMessageDialog(mainFrame, "Success! " + pendingVerificationEmail + " marked as verified.", "Verification Simulated", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        toLoginBtn.addActionListener(e -> showScreen(SCREEN_LOGIN));

        card.add(Box.createVerticalStrut(12));
        card.add(titleLabel);
        card.add(Box.createVerticalStrut(4));
        card.add(subtitleLabel);
        card.add(Box.createVerticalStrut(12));
        card.add(verifyTargetEmailLabel);
        card.add(Box.createVerticalStrut(8));
        card.add(verifyStatusBadgeLabel);
        card.add(Box.createVerticalStrut(18));
        card.add(infoText);
        card.add(Box.createVerticalStrut(22));
        card.add(checkStatusBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(simulateVerifyBtn);
        card.add(Box.createVerticalStrut(10));
        card.add(toLoginBtn);
        card.add(Box.createVerticalStrut(12));

        panel.add(card);
        return panel;
    }

    private void refreshVerifyScreen() {
        if (!pendingVerificationEmail.isEmpty()) {
            verifyTargetEmailLabel.setText("Target: " + pendingVerificationEmail);
            boolean isV = authService.isEmailVerified(pendingVerificationEmail);
            if (isV) {
                verifyStatusBadgeLabel.setText("STATUS: VERIFIED ✔");
                verifyStatusBadgeLabel.setForeground(COLOR_SUCCESS);
            } else {
                verifyStatusBadgeLabel.setText("STATUS: PENDING VERIFICATION ⏳");
                verifyStatusBadgeLabel.setForeground(COLOR_WARNING);
            }
        }
    }

    // =========================================================================
    // SCREEN 4: Main Dashboard Screen
    // =========================================================================
    private JPanel createDashboardPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBackground(COLOR_CREAM);
        contentPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        // North Header Section: Greeting, Subtitle and Action Button
        JPanel headerSection = new JPanel();
        headerSection.setLayout(new BoxLayout(headerSection, BoxLayout.Y_AXIS));
        headerSection.setOpaque(false);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel titleBlock = new JPanel();
        titleBlock.setLayout(new BoxLayout(titleBlock, BoxLayout.Y_AXIS));
        titleBlock.setOpaque(false);

        JLabel mainHeading = new JLabel("Campus Rides, Made Easier");
        mainHeading.setFont(new Font("Segoe UI", Font.BOLD, 24));
        mainHeading.setForeground(COLOR_FOREST_GREEN);

        dashboardGreetingLabel = new JLabel("Welcome back to CampusRide!");
        dashboardGreetingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        dashboardGreetingLabel.setForeground(COLOR_MAIN_TEXT);

        titleBlock.add(mainHeading);
        titleBlock.add(Box.createVerticalStrut(4));
        titleBlock.add(dashboardGreetingLabel);

        JButton offerQuickBtn = createPrimaryButton("+ Offer a Ride");
        offerQuickBtn.addActionListener(e -> showScreen(SCREEN_OFFER_RIDE));

        topRow.add(titleBlock, BorderLayout.WEST);
        topRow.add(offerQuickBtn, BorderLayout.EAST);

        headerSection.add(topRow);
        headerSection.add(Box.createVerticalStrut(18));

        // Real Data Summary Cards (Zero fake statistics, strictly active counts)
        JPanel statsRow = new JPanel(new GridLayout(1, 4, 12, 0));
        statsRow.setOpaque(false);

        statAvailableCountLabel = new JLabel("0 Available");
        statMyRidesCountLabel = new JLabel("0 Offered");
        statPendingRequestsLabel = new JLabel("0 Pending");
        statVerificationBadgeLabel = new JLabel("Verified ✔");

        statsRow.add(createStatCard("AVAILABLE CAMPUS RIDES", statAvailableCountLabel, "Active community listings"));
        statsRow.add(createStatCard("MY POSTED RIDES", statMyRidesCountLabel, "Trips offered by you"));
        statsRow.add(createStatCard("PENDING REQUESTS", statPendingRequestsLabel, "Incoming passenger actions"));
        statsRow.add(createStatCard("STUDENT STATUS", statVerificationBadgeLabel, "NSSCE Verified Student"));

        headerSection.add(statsRow);
        contentPanel.add(headerSection, BorderLayout.NORTH);

        // Center: Available Rides Table in Ivory Card Container
        JPanel tableContainer = new JPanel(new BorderLayout(8, 12)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_IVORY_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tableContainer.setOpaque(false);
        tableContainer.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel tableHeaderPanel = new JPanel(new BorderLayout());
        tableHeaderPanel.setOpaque(false);

        JLabel tableTitle = new JLabel("Available Campus Rides");
        tableTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        tableTitle.setForeground(COLOR_FOREST_GREEN);

        JLabel tableSub = new JLabel("Browse route details, timings, fares and reserve your seat");
        tableSub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tableSub.setForeground(COLOR_MUTED_TEXT);

        JPanel tableTitlesWrapper = new JPanel();
        tableTitlesWrapper.setLayout(new BoxLayout(tableTitlesWrapper, BoxLayout.Y_AXIS));
        tableTitlesWrapper.setOpaque(false);
        tableTitlesWrapper.add(tableTitle);
        tableTitlesWrapper.add(Box.createVerticalStrut(2));
        tableTitlesWrapper.add(tableSub);

        tableHeaderPanel.add(tableTitlesWrapper, BorderLayout.WEST);

        String[] cols = {"Ride ID", "Driver", "From", "To", "Date", "Time", "Seats Left", "Fare ($)", "Notes"};
        dashboardRidesModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(dashboardRidesModel);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(COLOR_IVORY_CARD);

        JPanel tableBottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tableBottom.setOpaque(false);

        JButton refreshDashBtn = createOutlineButton("⟳ Refresh");
        refreshDashBtn.addActionListener(e -> refreshDashboard());

        JButton quickBookBtn = createPrimaryButton("Request Ride Seat");
        quickBookBtn.addActionListener(e -> {
            int selectedRow = table.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(mainFrame, "Please select a ride to request from the table.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String rideId = (String) dashboardRidesModel.getValueAt(selectedRow, 0);
            handleRequestRideAction(rideId);
        });

        tableBottom.add(refreshDashBtn);
        tableBottom.add(quickBookBtn);

        tableContainer.add(tableHeaderPanel, BorderLayout.NORTH);
        tableContainer.add(scrollPane, BorderLayout.CENTER);
        tableContainer.add(tableBottom, BorderLayout.SOUTH);

        contentPanel.add(tableContainer, BorderLayout.CENTER);

        return createAuthenticatedLayout(SCREEN_DASHBOARD, contentPanel);
    }

    private void refreshDashboard() {
        Student current = authService.getCurrentLoggedInStudent();
        if (current != null) {
            dashboardGreetingLabel.setText("Welcome back, " + current.getName() + " (" + current.getStudentId() + ")");
            boolean isV = authService.isEmailVerified(current.getEmail());
            statVerificationBadgeLabel.setText(isV ? "Verified ✔" : "Pending ⏳");
            statVerificationBadgeLabel.setForeground(isV ? COLOR_SUCCESS : COLOR_WARNING);
        }

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                rideService.syncRidesFromSupabase();
                if (current != null) {
                    rideService.syncRequestsFromSupabase(current);
                }
                return null;
            }

            @Override
            protected void done() {
                dashboardRidesModel.setRowCount(0);
                ArrayList<Ride> rides = rideService.getRides();
                statAvailableCountLabel.setText(rides.size() + (rides.size() == 1 ? " Ride" : " Rides"));

                if (current != null) {
                    int myCount = 0;
                    for (Ride r : rides) {
                        if (r.getDriver() != null) {
                            if ((current.getId() != null && current.getId().equalsIgnoreCase(r.getDriver().getId()))
                                    || (current.getStudentId() != null && current.getStudentId().equalsIgnoreCase(r.getDriver().getStudentId()))) {
                                myCount++;
                            }
                        }
                    }
                    statMyRidesCountLabel.setText(myCount + (myCount == 1 ? " Ride" : " Rides"));

                    String driverId = (current.getId() != null && !current.getId().isEmpty()) ? current.getId() : current.getStudentId();
                    ArrayList<RideRequest> incoming = rideService.getRequestsForDriver(driverId);
                    int pendingCount = 0;
                    for (RideRequest req : incoming) {
                        if (req.isPending()) pendingCount++;
                    }
                    statPendingRequestsLabel.setText(pendingCount + (pendingCount == 1 ? " Pending" : " Pending"));
                    statPendingRequestsLabel.setForeground(pendingCount > 0 ? COLOR_WARNING : COLOR_SUCCESS);
                }

                for (Ride r : rides) {
                    String driverName = (r.getDriver() != null && r.getDriver().getName() != null && !r.getDriver().getName().isEmpty())
                            ? r.getDriver().getName() : "Student Driver";
                    String notes = (r.getNotes() != null && !r.getNotes().isEmpty()) ? r.getNotes() : "-";
                    dashboardRidesModel.addRow(new Object[]{
                            r.getRideId(),
                            driverName,
                            r.getStartingLocation(),
                            r.getDestination(),
                            r.getDate(),
                            r.getDepartureTime(),
                            r.getAvailableSeats(),
                            String.format("%.2f", r.getCostPerPerson()),
                            notes
                    });
                }
            }
        };
        worker.execute();
    }

    // =========================================================================
    // SCREEN 5: Offer Ride Screen
    // =========================================================================
    private JPanel createOfferRidePanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBackground(COLOR_CREAM);
        contentPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel title = new JLabel("Offer a Campus Ride");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(COLOR_FOREST_GREEN);

        JLabel sub = new JLabel("Share empty seats and travel sustainably across NSSCE campus");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(COLOR_MUTED_TEXT);

        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        JButton backBtn = createOutlineButton("← Back to Dashboard");
        backBtn.addActionListener(e -> showScreen(SCREEN_DASHBOARD));

        header.add(titles, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        contentPanel.add(header, BorderLayout.NORTH);

        JPanel card = createCardPanel(680, 520);
        card.setLayout(new GridLayout(10, 2, 14, 8));

        JTextField startLocField = createStyledTextField();
        JTextField destField = createStyledTextField();
        JTextField dateField = createStyledTextField();
        dateField.setText("2026-10-15");
        JTextField timeField = createStyledTextField();
        timeField.setText("08:30 AM");

        JComboBox<String> vehicleTypeCombo = new JComboBox<>(new String[]{"Car", "Bike", "Scooter"});
        vehicleTypeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        vehicleTypeCombo.setBackground(Color.WHITE);
        vehicleTypeCombo.setForeground(COLOR_MAIN_TEXT);

        JTextField regNumField = createStyledTextField();
        regNumField.setText("KL-09-AB-1234");
        JTextField capacityField = createStyledTextField();
        capacityField.setText("4");
        JTextField seatsField = createStyledTextField();
        seatsField.setText("3");
        JTextField costField = createStyledTextField();
        costField.setText("20.00");
        JTextField notesField = createStyledTextField();
        notesField.setText("Leaving on time, luggage space available");

        JPanel costRow = new JPanel(new BorderLayout(8, 0));
        costRow.setOpaque(false);
        costRow.add(costField, BorderLayout.CENTER);
        JButton fuelCalcBtn = createSecondaryButton("⚡ Fuel Estimator");
        fuelCalcBtn.addActionListener(e -> showPetrolCostEstimatorDialog(costField));
        costRow.add(fuelCalcBtn, BorderLayout.EAST);

        card.add(createFormLabel("Starting Location:"));
        card.add(startLocField);
        card.add(createFormLabel("Destination:"));
        card.add(destField);
        card.add(createFormLabel("Date (YYYY-MM-DD):"));
        card.add(dateField);
        card.add(createFormLabel("Departure Time:"));
        card.add(timeField);
        card.add(createFormLabel("Vehicle Type:"));
        card.add(vehicleTypeCombo);
        card.add(createFormLabel("Vehicle Reg Number:"));
        card.add(regNumField);
        card.add(createFormLabel("Total Vehicle Capacity:"));
        card.add(capacityField);
        card.add(createFormLabel("Available Passenger Seats:"));
        card.add(seatsField);
        card.add(createFormLabel("Cost Per Person ($):"));
        card.add(costRow);
        card.add(createFormLabel("Optional Notes:"));
        card.add(notesField);

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setOpaque(false);
        JButton publishBtn = createPrimaryButton("Publish Ride to CampusRide");
        publishBtn.setPreferredSize(new Dimension(300, 42));

        publishBtn.addActionListener(e -> {
            Student currentStudent = authService.getCurrentLoggedInStudent();
            if (currentStudent == null) {
                JOptionPane.showMessageDialog(mainFrame, "Session expired. Please log in again.", "Error", JOptionPane.ERROR_MESSAGE);
                showScreen(SCREEN_LOGIN);
                return;
            }

            try {
                String startLoc = startLocField.getText().trim();
                String dest = destField.getText().trim();
                String date = dateField.getText().trim();
                String time = timeField.getText().trim();
                String vType = (String) vehicleTypeCombo.getSelectedItem();
                String vNumber = regNumField.getText().trim();
                String notes = notesField.getText().trim();
                int capacity = Integer.parseInt(capacityField.getText().trim());
                int availableSeats = Integer.parseInt(seatsField.getText().trim());
                double cost = Double.parseDouble(costField.getText().trim());

                if (startLoc.isEmpty() || dest.isEmpty() || date.isEmpty() || time.isEmpty() || vNumber.isEmpty()) {
                    JOptionPane.showMessageDialog(mainFrame, "All fields are required.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                String rideId = java.util.UUID.randomUUID().toString();

                Vehicle vehicle = new Vehicle(vType, vNumber, capacity);
                Driver driver = new Driver(
                        currentStudent.getId(),
                        currentStudent.getName(),
                        currentStudent.getStudentId(),
                        currentStudent.getEmail(),
                        currentStudent.getPassword(),
                        vehicle
                );

                Ride ride = new Ride(rideId, driver, startLoc, dest, date, time, availableSeats, cost, notes);

                publishBtn.setEnabled(false);
                publishBtn.setText("Publishing to Supabase...");

                SwingWorker<Boolean, Void> offerWorker = new SwingWorker<>() {
                    @Override
                    protected Boolean doInBackground() {
                        rideService.offerRide(ride);
                        fileService.saveRide(ride);
                        return true;
                    }

                    @Override
                    protected void done() {
                        publishBtn.setEnabled(true);
                        publishBtn.setText("Publish Ride to CampusRide");
                        JOptionPane.showMessageDialog(mainFrame, "Ride published successfully! ID: " + rideId, "Success", JOptionPane.INFORMATION_MESSAGE);
                        startLocField.setText("");
                        destField.setText("");
                        notesField.setText("");
                        showScreen(SCREEN_DASHBOARD);
                    }
                };
                offerWorker.execute();

            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(mainFrame, "Please enter valid numbers for capacity, seats, and cost.", "Numeric Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        bottomPanel.add(publishBtn);

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(card);

        contentPanel.add(centerWrapper, BorderLayout.CENTER);
        contentPanel.add(bottomPanel, BorderLayout.SOUTH);

        return createAuthenticatedLayout(SCREEN_OFFER_RIDE, contentPanel);
    }

    private JLabel createFormLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(COLOR_MAIN_TEXT);
        return label;
    }

    // =========================================================================
    // SCREEN 6: Search Ride Screen
    // =========================================================================
    private JPanel createSearchRidePanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBackground(COLOR_CREAM);
        contentPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel title = new JLabel("Search Available Campus Rides");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(COLOR_FOREST_GREEN);

        JLabel sub = new JLabel("Filter rides by starting location, destination or travel date");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(COLOR_MUTED_TEXT);

        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        JButton backBtn = createOutlineButton("← Back to Dashboard");
        backBtn.addActionListener(e -> showScreen(SCREEN_DASHBOARD));
        header.add(titles, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        contentPanel.add(header, BorderLayout.NORTH);

        // Search Criteria Filter Card
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_IVORY_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        filterCard.setOpaque(false);
        filterCard.setBorder(new EmptyBorder(8, 12, 8, 12));

        JTextField srcField = createStyledTextField();
        srcField.setColumns(12);
        JTextField dstField = createStyledTextField();
        dstField.setColumns(12);
        JTextField dateField = createStyledTextField();
        dateField.setColumns(10);

        JButton searchBtn = createPrimaryButton("Search Route");
        JButton showAllBtn = createSecondaryButton("Show All Rides");

        filterCard.add(createFormLabel("From:"));
        filterCard.add(srcField);
        filterCard.add(createFormLabel("To:"));
        filterCard.add(dstField);
        filterCard.add(createFormLabel("Date (Optional):"));
        filterCard.add(dateField);
        filterCard.add(searchBtn);
        filterCard.add(showAllBtn);

        // Search Results Table
        String[] cols = {"Ride ID", "Driver", "From", "To", "Date", "Time", "Seats Left", "Fare ($)", "Notes"};
        searchResultsModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        JTable table = new JTable(searchResultsModel);
        styleTable(table);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        scrollPane.getViewport().setBackground(COLOR_IVORY_CARD);

        JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        tableActions.setOpaque(false);
        JButton bookSelectedBtn = createPrimaryButton("Request Selected Ride");

        searchBtn.addActionListener(e -> {
            String src = srcField.getText().trim();
            String dst = dstField.getText().trim();
            String date = dateField.getText().trim();

            if (src.isEmpty() || dst.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "Please enter both From (source) and To (destination).", "Search Filter", JOptionPane.WARNING_MESSAGE);
                return;
            }

            searchResultsModel.setRowCount(0);
            ArrayList<Ride> results;

            if (!date.isEmpty()) {
                rideService.searchRide(src, dst, date);
                results = rideService.findRides(src, dst, date);
            } else {
                rideService.searchRide(src, dst);
                results = rideService.findRides(src, dst);
            }

            for (Ride r : results) {
                String driverName = (r.getDriver() != null && r.getDriver().getName() != null && !r.getDriver().getName().isEmpty())
                        ? r.getDriver().getName() : "Student Driver";
                String notes = (r.getNotes() != null && !r.getNotes().isEmpty()) ? r.getNotes() : "-";
                searchResultsModel.addRow(new Object[]{
                        r.getRideId(),
                        driverName,
                        r.getStartingLocation(),
                        r.getDestination(),
                        r.getDate(),
                        r.getDepartureTime(),
                        r.getAvailableSeats(),
                        String.format("%.2f", r.getCostPerPerson()),
                        notes
                });
            }

            if (results.isEmpty()) {
                JOptionPane.showMessageDialog(mainFrame, "No rides found matching your route.", "Search Results", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        showAllBtn.addActionListener(e -> {
            searchResultsModel.setRowCount(0);
            for (Ride r : rideService.getRides()) {
                String driverName = (r.getDriver() != null && r.getDriver().getName() != null && !r.getDriver().getName().isEmpty())
                        ? r.getDriver().getName() : "Student Driver";
                String notes = (r.getNotes() != null && !r.getNotes().isEmpty()) ? r.getNotes() : "-";
                searchResultsModel.addRow(new Object[]{
                        r.getRideId(),
                        driverName,
                        r.getStartingLocation(),
                        r.getDestination(),
                        r.getDate(),
                        r.getDepartureTime(),
                        r.getAvailableSeats(),
                        String.format("%.2f", r.getCostPerPerson()),
                        notes
                });
            }
        });

        bookSelectedBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(mainFrame, "Please select a ride from the table to request.", "Notice", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String rideId = (String) searchResultsModel.getValueAt(row, 0);
            handleRequestRideAction(rideId);
        });

        tableActions.add(bookSelectedBtn);

        JPanel centerArea = new JPanel(new BorderLayout(10, 10));
        centerArea.setOpaque(false);
        centerArea.add(filterCard, BorderLayout.NORTH);
        centerArea.add(scrollPane, BorderLayout.CENTER);
        centerArea.add(tableActions, BorderLayout.SOUTH);

        contentPanel.add(centerArea, BorderLayout.CENTER);
        return createAuthenticatedLayout(SCREEN_SEARCH_RIDE, contentPanel);
    }

    // =========================================================================
    // SCREEN 7: My Rides Screen (Driver Offers, Incoming Approvals, Passenger Requests)
    // =========================================================================
    private JPanel createMyRidesPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(14, 14));
        contentPanel.setBackground(COLOR_CREAM);
        contentPanel.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel title = new JLabel("My Rides & Passenger Requests");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(COLOR_FOREST_GREEN);

        JLabel sub = new JLabel("Manage rides offered by you, review approvals, and track your submitted ride requests");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(COLOR_MUTED_TEXT);

        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        JButton backBtn = createOutlineButton("← Back to Dashboard");
        backBtn.addActionListener(e -> showScreen(SCREEN_DASHBOARD));
        header.add(titles, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        contentPanel.add(header, BorderLayout.NORTH);

        // Modern 3-Tab Interface
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(COLOR_IVORY_CARD);
        tabbedPane.setForeground(COLOR_FOREST_GREEN);

        // -------------------------------------------------------------
        // TAB 1: Rides Offered by You (Driver View)
        // -------------------------------------------------------------
        JPanel tabOffered = new JPanel(new BorderLayout(10, 10));
        tabOffered.setBackground(COLOR_IVORY_CARD);
        tabOffered.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel offeredTitle = new JLabel("Rides Offered by You (Driver Listings)");
        offeredTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        offeredTitle.setForeground(COLOR_FOREST_GREEN);
        tabOffered.add(offeredTitle, BorderLayout.NORTH);

        String[] colsOffered = {"Ride ID", "From", "To", "Date", "Departure Time", "Seats Left", "Fare ($)", "Notes"};
        myRidesModel = new DefaultTableModel(colsOffered, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tableOffered = new JTable(myRidesModel);
        styleTable(tableOffered);
        JScrollPane scrollOffered = new JScrollPane(tableOffered);
        scrollOffered.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        scrollOffered.getViewport().setBackground(COLOR_IVORY_CARD);
        tabOffered.add(scrollOffered, BorderLayout.CENTER);

        JPanel offeredActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        offeredActions.setOpaque(false);

        JButton refreshOfferedBtn = createOutlineButton("⟳ Refresh Rides");
        refreshOfferedBtn.addActionListener(e -> refreshMyRides());

        JButton cancelRideBtn = createDangerButton("✕ Cancel Selected Ride");
        cancelRideBtn.addActionListener(e -> {
            int row = tableOffered.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(mainFrame, "Please select an offered ride to cancel.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String rideId = (String) myRidesModel.getValueAt(row, 0);
            int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Are you sure you want to cancel Ride ID: " + rideId + "?\nThis will remove it from active campus listings.",
                    "Confirm Cancellation", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            cancelRideBtn.setEnabled(false);
            cancelRideBtn.setText("Cancelling...");

            Student current = authService.getCurrentLoggedInStudent();
            String driverId = (current != null && current.getId() != null && !current.getId().isEmpty())
                    ? current.getId() : (current != null ? current.getStudentId() : null);

            SwingWorker<Boolean, Void> cancelWorker = new SwingWorker<>() {
                private String errMsg = null;
                @Override
                protected Boolean doInBackground() {
                    try {
                        boolean ok = rideService.cancelRide(rideId, driverId);
                        if (ok) {
                            fileService.saveAllRides(rideService.getRides());
                        }
                        return ok;
                    } catch (Exception ex) {
                        errMsg = ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    cancelRideBtn.setEnabled(true);
                    cancelRideBtn.setText("✕ Cancel Selected Ride");
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(mainFrame, "Ride " + rideId + " has been cancelled.", "Success", JOptionPane.INFORMATION_MESSAGE);
                            refreshMyRides();
                            refreshDashboard();
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, errMsg != null ? errMsg : "Could not cancel ride.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainFrame, "Cancellation error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            cancelWorker.execute();
        });

        offeredActions.add(refreshOfferedBtn);
        offeredActions.add(cancelRideBtn);
        tabOffered.add(offeredActions, BorderLayout.SOUTH);

        // -------------------------------------------------------------
        // TAB 2: Incoming Passenger Requests (Driver Approvals)
        // -------------------------------------------------------------
        JPanel tabIncoming = new JPanel(new BorderLayout(10, 10));
        tabIncoming.setBackground(COLOR_IVORY_CARD);
        tabIncoming.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel reqHeader = new JPanel(new BorderLayout());
        reqHeader.setOpaque(false);

        JLabel reqTitle = new JLabel("Incoming Passenger Requests (Driver Approvals)");
        reqTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        reqTitle.setForeground(COLOR_FOREST_GREEN);

        JLabel reqSub = new JLabel("Approve or decline requests. Confirmed pickup & drop-off locations are shown below.");
        reqSub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        reqSub.setForeground(COLOR_MUTED_TEXT);

        JPanel reqTitleBox = new JPanel();
        reqTitleBox.setLayout(new BoxLayout(reqTitleBox, BoxLayout.Y_AXIS));
        reqTitleBox.setOpaque(false);
        reqTitleBox.add(reqTitle);
        reqTitleBox.add(reqSub);
        reqHeader.add(reqTitleBox, BorderLayout.WEST);
        tabIncoming.add(reqHeader, BorderLayout.NORTH);

        String[] colsReq = {"Request ID", "Ride ID", "Requester Name", "Student ID", "Pickup Location", "Drop-off", "Seats", "Status", "Requested At"};
        incomingRequestsModel = new DefaultTableModel(colsReq, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tableReq = new JTable(incomingRequestsModel);
        styleTable(tableReq);
        JScrollPane scrollReq = new JScrollPane(tableReq);
        scrollReq.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        scrollReq.getViewport().setBackground(COLOR_IVORY_CARD);
        tabIncoming.add(scrollReq, BorderLayout.CENTER);

        JPanel reqActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        reqActions.setOpaque(false);

        JButton refreshReqBtn = createOutlineButton("⟳ Refresh");
        refreshReqBtn.addActionListener(e -> refreshMyRides());

        JButton declineBtn = createSecondaryButton("✕ Decline Request");
        declineBtn.addActionListener(e -> {
            int row = tableReq.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(mainFrame, "Please select an incoming request from the table to decline.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String reqId = (String) incomingRequestsModel.getValueAt(row, 0);
            String status = (String) incomingRequestsModel.getValueAt(row, 7);
            if (!"PENDING".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(mainFrame, "Only PENDING requests can be declined (current: " + status + ").", "Notice", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to decline request " + reqId + "?", "Confirm Decline", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;

            declineBtn.setEnabled(false);
            declineBtn.setText("Declining...");

            SwingWorker<Boolean, Void> declineWorker = new SwingWorker<>() {
                private String errMsg = null;
                @Override
                protected Boolean doInBackground() {
                    try {
                        return rideService.declineRequest(reqId);
                    } catch (Exception ex) {
                        errMsg = ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    declineBtn.setEnabled(true);
                    declineBtn.setText("✕ Decline Request");
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(mainFrame, "Passenger request was declined.", "Request Declined", JOptionPane.INFORMATION_MESSAGE);
                            refreshMyRides();
                            refreshDashboard();
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, errMsg != null ? errMsg : "Action failed", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainFrame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            declineWorker.execute();
        });

        JButton acceptBtn = createPrimaryButton("✓ Accept Request");
        acceptBtn.addActionListener(e -> {
            int row = tableReq.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(mainFrame, "Please select an incoming request from the table to accept.", "Selection Required", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String reqId = (String) incomingRequestsModel.getValueAt(row, 0);
            String status = (String) incomingRequestsModel.getValueAt(row, 7);
            if (!"PENDING".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(mainFrame, "Only PENDING requests can be accepted (current: " + status + ").", "Notice", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            acceptBtn.setEnabled(false);
            acceptBtn.setText("Accepting...");

            SwingWorker<Boolean, Void> acceptWorker = new SwingWorker<>() {
                private String errMsg = null;
                @Override
                protected Boolean doInBackground() {
                    try {
                        boolean ok = rideService.acceptRequest(reqId);
                        if (ok) {
                            fileService.saveAllRides(rideService.getRides());
                        }
                        return ok;
                    } catch (Exception ex) {
                        errMsg = ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    acceptBtn.setEnabled(true);
                    acceptBtn.setText("✓ Accept Request");
                    try {
                        if (get()) {
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Passenger request accepted successfully!\n"
                                    + "Seats have been updated and confirmed in Supabase.",
                                    "Request Accepted", JOptionPane.INFORMATION_MESSAGE);
                            refreshMyRides();
                            refreshDashboard();
                        } else {
                            JOptionPane.showMessageDialog(mainFrame, errMsg != null ? errMsg : "Action failed", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(mainFrame, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            acceptWorker.execute();
        });

        reqActions.add(refreshReqBtn);
        reqActions.add(declineBtn);
        reqActions.add(acceptBtn);
        tabIncoming.add(reqActions, BorderLayout.SOUTH);

        // -------------------------------------------------------------
        // TAB 3: My Submitted Requests (Passenger View)
        // -------------------------------------------------------------
        JPanel tabSubmitted = new JPanel(new BorderLayout(10, 10));
        tabSubmitted.setBackground(COLOR_IVORY_CARD);
        tabSubmitted.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JPanel subHeader = new JPanel(new BorderLayout());
        subHeader.setOpaque(false);

        JLabel subTitle = new JLabel("My Submitted Requests (Passenger View)");
        subTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        subTitle.setForeground(COLOR_FOREST_GREEN);

        JLabel subHelp = new JLabel("Track status (Pending, Accepted, Declined) and view confirmed pickup / drop-off locations");
        subHelp.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subHelp.setForeground(COLOR_MUTED_TEXT);

        JPanel subTitleBox = new JPanel();
        subTitleBox.setLayout(new BoxLayout(subTitleBox, BoxLayout.Y_AXIS));
        subTitleBox.setOpaque(false);
        subTitleBox.add(subTitle);
        subTitleBox.add(subHelp);
        subHeader.add(subTitleBox, BorderLayout.WEST);
        tabSubmitted.add(subHeader, BorderLayout.NORTH);

        String[] colsSub = {"Request ID", "Ride ID", "Driver", "Pickup Location", "Drop-off", "Seats", "Status", "Requested At"};
        mySubmittedRequestsModel = new DefaultTableModel(colsSub, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        JTable tableSub = new JTable(mySubmittedRequestsModel);
        styleTable(tableSub);
        JScrollPane scrollSub = new JScrollPane(tableSub);
        scrollSub.setBorder(BorderFactory.createLineBorder(COLOR_BORDER, 1));
        scrollSub.getViewport().setBackground(COLOR_IVORY_CARD);
        tabSubmitted.add(scrollSub, BorderLayout.CENTER);

        JPanel subActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        subActions.setOpaque(false);

        JButton refreshSubBtn = createOutlineButton("⟳ Refresh Status");
        refreshSubBtn.addActionListener(e -> refreshMyRides());
        subActions.add(refreshSubBtn);
        tabSubmitted.add(subActions, BorderLayout.SOUTH);

        // Add all 3 tabs
        tabbedPane.addTab("🚘 Rides Offered (Driver)", tabOffered);
        tabbedPane.addTab("📥 Incoming Requests (Driver Approvals)", tabIncoming);
        tabbedPane.addTab("📋 My Requests (Passenger Tracking)", tabSubmitted);

        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        return createAuthenticatedLayout(SCREEN_MY_RIDES, contentPanel);
    }

    private void refreshMyRides() {
        Student current = authService.getCurrentLoggedInStudent();
        if (current == null) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                rideService.syncRidesFromSupabase();
                rideService.syncRequestsFromSupabase(current);
                return null;
            }

            @Override
            protected void done() {
                populateMyRidesTables(current);
            }
        };
        worker.execute();
    }

    private void populateMyRidesTables(Student current) {
        if (current == null) return;

        // 1. Populate rides offered by current driver
        myRidesModel.setRowCount(0);
        ArrayList<Ride> userRides = rideService.getRidesByDriver(current.getStudentId());
        for (Ride r : rideService.getRides()) {
            if (r.getDriver() != null) {
                boolean match = (current.getId() != null && current.getId().equalsIgnoreCase(r.getDriver().getId()))
                        || (current.getStudentId() != null && current.getStudentId().equalsIgnoreCase(r.getDriver().getStudentId()));
                if (match && !userRides.contains(r)) {
                    userRides.add(r);
                }
            }
        }

        for (Ride r : userRides) {
            String notes = (r.getNotes() != null && !r.getNotes().isEmpty()) ? r.getNotes() : "-";
            myRidesModel.addRow(new Object[]{
                    r.getRideId(),
                    r.getStartingLocation(),
                    r.getDestination(),
                    r.getDate(),
                    r.getDepartureTime(),
                    r.getAvailableSeats(),
                    String.format("%.2f", r.getCostPerPerson()),
                    notes
            });
        }

        // 2. Populate incoming passenger requests for current driver
        if (incomingRequestsModel != null) {
            incomingRequestsModel.setRowCount(0);
            String driverIdentifier = (current.getId() != null && !current.getId().trim().isEmpty())
                    ? current.getId()
                    : current.getStudentId();
            ArrayList<RideRequest> incoming = rideService.getRequestsForDriver(driverIdentifier);

            if (current.getId() != null && !current.getId().trim().isEmpty()) {
                ArrayList<RideRequest> extra = rideService.getRequestsForDriver(current.getStudentId());
                for (RideRequest req : extra) {
                    if (!incoming.contains(req)) {
                        incoming.add(req);
                    }
                }
            }

            int pendingCount = 0;
            for (RideRequest req : incoming) {
                if (req.isPending()) pendingCount++;
                String reqName = (req.getRequester() != null && req.getRequester().getName() != null)
                        ? req.getRequester().getName() : "Student";
                String reqRoll = (req.getRequester() != null && req.getRequester().getStudentId() != null)
                        ? req.getRequester().getStudentId() : (req.getRequesterId() != null ? req.getRequesterId() : "N/A");
                String pickup = (req.getPickupLocation() != null && !req.getPickupLocation().isEmpty())
                        ? req.getPickupLocation() : "-";
                String dropoff = (req.getDropoffLocation() != null && !req.getDropoffLocation().isEmpty())
                        ? req.getDropoffLocation() : "-";
                String created = (req.getCreatedAt() != null) ? req.getCreatedAt() : "Recent";

                incomingRequestsModel.addRow(new Object[]{
                        req.getRequestId(),
                        req.getRideId(),
                        reqName,
                        reqRoll,
                        pickup,
                        dropoff,
                        req.getSeatsRequested(),
                        req.getStatus(),
                        created
                });
            }

            if (statPendingRequestsLabel != null) {
                statPendingRequestsLabel.setText(pendingCount + (pendingCount == 1 ? " Pending" : " Pending"));
                statPendingRequestsLabel.setForeground(pendingCount > 0 ? COLOR_WARNING : COLOR_SUCCESS);
            }
        }

        // 3. Populate submitted requests (Passenger View)
        if (mySubmittedRequestsModel != null) {
            mySubmittedRequestsModel.setRowCount(0);
            String requesterIdentifier = (current.getId() != null && !current.getId().trim().isEmpty())
                    ? current.getId()
                    : current.getStudentId();
            ArrayList<RideRequest> myReqs = rideService.getRequestsByRequester(requesterIdentifier);

            if (current.getId() != null && !current.getId().trim().isEmpty()) {
                ArrayList<RideRequest> extra = rideService.getRequestsByRequester(current.getStudentId());
                for (RideRequest req : extra) {
                    if (!myReqs.contains(req)) {
                        myReqs.add(req);
                    }
                }
            }

            for (RideRequest req : myReqs) {
                String driverName = "Driver";
                try {
                    Ride r = rideService.findRideById(req.getRideId());
                    if (r.getDriver() != null && r.getDriver().getName() != null) {
                        driverName = r.getDriver().getName();
                    }
                } catch (Exception ignored) {}

                String pickup = (req.getPickupLocation() != null && !req.getPickupLocation().isEmpty())
                        ? req.getPickupLocation() : "-";
                String dropoff = (req.getDropoffLocation() != null && !req.getDropoffLocation().isEmpty())
                        ? req.getDropoffLocation() : "-";
                String created = (req.getCreatedAt() != null) ? req.getCreatedAt() : "Recent";

                mySubmittedRequestsModel.addRow(new Object[]{
                        req.getRequestId(),
                        req.getRideId(),
                        driverName,
                        pickup,
                        dropoff,
                        req.getSeatsRequested(),
                        req.getStatus(),
                        created
                });
            }
        }
    }

    /**
     * Handles modal dialog for requesting a ride seat with custom pickup location.
     */
    private void handleRequestRideAction(String rideId) {
        Student currentStudent = authService.getCurrentLoggedInStudent();
        if (currentStudent == null) {
            JOptionPane.showMessageDialog(mainFrame, "Please log in to request a ride seat.", "Login Required", JOptionPane.WARNING_MESSAGE);
            showScreen(SCREEN_LOGIN);
            return;
        }

        Ride ride;
        try {
            ride = rideService.findRideById(rideId);
        } catch (RideNotFoundException ex) {
            JOptionPane.showMessageDialog(mainFrame, "Ride not found: " + rideId, "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (ride.getDriver() != null) {
            boolean sameUuid = currentStudent.getId() != null && !currentStudent.getId().isEmpty()
                    && currentStudent.getId().equalsIgnoreCase(ride.getDriver().getId());
            boolean sameReg = currentStudent.getStudentId() != null
                    && currentStudent.getStudentId().equalsIgnoreCase(ride.getDriver().getStudentId());
            if (sameUuid || sameReg) {
                JOptionPane.showMessageDialog(mainFrame, "You cannot request your own offered ride.", "Self-Request Prevented", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (ride.getAvailableSeats() <= 0) {
            JOptionPane.showMessageDialog(mainFrame, "This ride has no available seats remaining.", "Ride Full", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Modal dialog
        JDialog dialog = new JDialog(mainFrame, "Request Seat - Ride " + ride.getRideId().substring(0, Math.min(8, ride.getRideId().length())), true);
        dialog.setSize(520, 480);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setResizable(false);

        JPanel dialogPanel = new JPanel(new BorderLayout(14, 14));
        dialogPanel.setBackground(COLOR_CREAM);
        dialogPanel.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setOpaque(false);

        JLabel dlgTitle = new JLabel("Reserve Your Seat");
        dlgTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        dlgTitle.setForeground(COLOR_FOREST_GREEN);

        JLabel dlgSub = new JLabel("Route: " + ride.getStartingLocation() + "  ➔  " + ride.getDestination());
        dlgSub.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dlgSub.setForeground(COLOR_MAIN_TEXT);

        JLabel dlgInfo = new JLabel("Date: " + ride.getDate() + " | Time: " + ride.getDepartureTime() + " | Fare: $" + String.format("%.2f", ride.getCostPerPerson()));
        dlgInfo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        dlgInfo.setForeground(COLOR_MUTED_TEXT);

        topPanel.add(dlgTitle);
        topPanel.add(Box.createVerticalStrut(4));
        topPanel.add(dlgSub);
        topPanel.add(Box.createVerticalStrut(2));
        topPanel.add(dlgInfo);
        dialogPanel.add(topPanel, BorderLayout.NORTH);

        JPanel formCard = createCardPanel(460, 260);
        formCard.setLayout(new GridLayout(5, 2, 10, 10));

        JTextField pickupField = createStyledTextField();
        pickupField.setText(ride.getStartingLocation());

        JLabel dropoffDisplay = new JLabel(ride.getDestination());
        dropoffDisplay.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dropoffDisplay.setForeground(COLOR_MAIN_TEXT);

        int maxSeats = Math.max(1, Math.min(ride.getAvailableSeats(), 6));
        Integer[] seatOptions = new Integer[maxSeats];
        for (int i = 0; i < maxSeats; i++) seatOptions[i] = i + 1;
        JComboBox<Integer> seatsCombo = new JComboBox<>(seatOptions);
        seatsCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel driverDisplay = new JLabel(ride.getDriver() != null && ride.getDriver().getName() != null ? ride.getDriver().getName() : "Student Driver");
        driverDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JLabel seatsAvailableDisplay = new JLabel(ride.getAvailableSeats() + " seats available");
        seatsAvailableDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        seatsAvailableDisplay.setForeground(COLOR_SUCCESS);

        formCard.add(createFormLabel("Driver:"));
        formCard.add(driverDisplay);
        formCard.add(createFormLabel("Available Capacity:"));
        formCard.add(seatsAvailableDisplay);
        formCard.add(createFormLabel("Your Pickup Location:"));
        formCard.add(pickupField);
        formCard.add(createFormLabel("Drop-off Location:"));
        formCard.add(dropoffDisplay);
        formCard.add(createFormLabel("Seats to Reserve:"));
        formCard.add(seatsCombo);

        dialogPanel.add(formCard, BorderLayout.CENTER);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnRow.setOpaque(false);

        JButton cancelDlgBtn = createOutlineButton("Cancel");
        cancelDlgBtn.addActionListener(e -> dialog.dispose());

        JButton submitDlgBtn = createPrimaryButton("Submit Request");
        submitDlgBtn.addActionListener(e -> {
            String pickup = pickupField.getText().trim();
            if (pickup.isEmpty()) {
                JOptionPane.showMessageDialog(dialog, "Please enter a pickup location.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int seats = (Integer) seatsCombo.getSelectedItem();

            submitDlgBtn.setEnabled(false);
            submitDlgBtn.setText("Submitting...");

            SwingWorker<Boolean, Void> reqWorker = new SwingWorker<>() {
                private String errorMsg = null;

                @Override
                protected Boolean doInBackground() {
                    try {
                        boolean ok = rideService.requestRide(rideId, currentStudent, seats, pickup);
                        if (ok) {
                            fileService.saveAllRides(rideService.getRides());
                        }
                        return ok;
                    } catch (Exception ex) {
                        errorMsg = ex.getMessage();
                        return false;
                    }
                }

                @Override
                protected void done() {
                    submitDlgBtn.setEnabled(true);
                    submitDlgBtn.setText("Submit Request");
                    try {
                        boolean ok = get();
                        if (ok) {
                            dialog.dispose();
                            JOptionPane.showMessageDialog(mainFrame,
                                    "Your ride request has been submitted to the driver!\n\n"
                                    + "Pickup: " + pickup + "\n"
                                    + "Drop-off: " + ride.getDestination() + "\n"
                                    + "Seats Requested: " + seats + "\n"
                                    + "Status: PENDING\n\n"
                                    + "The driver will review and approve your reservation in 'My Rides'.",
                                    "Request Submitted Successfully", JOptionPane.INFORMATION_MESSAGE);
                            refreshDashboard();
                            refreshMyRides();
                        } else {
                            JOptionPane.showMessageDialog(dialog, errorMsg != null ? errorMsg : "Request could not be submitted.", "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(dialog, "Failed to submit request: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            };
            reqWorker.execute();
        });

        btnRow.add(cancelDlgBtn);
        btnRow.add(submitDlgBtn);
        dialogPanel.add(btnRow, BorderLayout.SOUTH);

        dialog.setContentPane(dialogPanel);
        dialog.setVisible(true);
    }

    /**
     * Opens modal dialog calculating Petrol and Shared Ride Cost.
     */
    private void showPetrolCostEstimatorDialog(JTextField targetPriceField) {
        JDialog dialog = new JDialog(mainFrame, "CampusRide — Petrol & Trip Cost Estimator", true);
        dialog.setSize(540, 520);
        dialog.setLocationRelativeTo(mainFrame);
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(14, 14));
        panel.setBackground(COLOR_CREAM);
        panel.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);

        JLabel title = new JLabel("⛽ Petrol & Shared Cost Estimator");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(COLOR_FOREST_GREEN);

        JLabel sub = new JLabel("Calculate fair fuel expense: Cost = (Distance / Mileage) × Fuel Price");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(COLOR_MUTED_TEXT);

        header.add(title);
        header.add(Box.createVerticalStrut(3));
        header.add(sub);
        panel.add(header, BorderLayout.NORTH);

        JPanel inputsCard = createCardPanel(480, 220);
        inputsCard.setLayout(new GridLayout(4, 2, 12, 10));

        JTextField distField = createStyledTextField();
        distField.setText("25.0");
        JTextField mileageField = createStyledTextField();
        mileageField.setText("15.0");
        JTextField fuelPriceField = createStyledTextField();
        fuelPriceField.setText("105.0");
        JTextField peopleField = createStyledTextField();
        peopleField.setText("3");

        inputsCard.add(createFormLabel("Estimated Distance (km):"));
        inputsCard.add(distField);
        inputsCard.add(createFormLabel("Vehicle Mileage (km/Litre):"));
        inputsCard.add(mileageField);
        inputsCard.add(createFormLabel("Petrol Price per Litre ($/₹):"));
        inputsCard.add(fuelPriceField);
        inputsCard.add(createFormLabel("Number of People Sharing:"));
        inputsCard.add(peopleField);

        JPanel resultBox = new JPanel();
        resultBox.setLayout(new BoxLayout(resultBox, BoxLayout.Y_AXIS));
        resultBox.setBackground(COLOR_IVORY_CARD);
        resultBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel totalCostLabel = new JLabel("Estimated Petrol Cost: ₹ 175.00");
        totalCostLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        totalCostLabel.setForeground(COLOR_FOREST_GREEN);

        JLabel perPersonLabel = new JLabel("Cost Per Person (Estimate): ₹ 58.33");
        perPersonLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        perPersonLabel.setForeground(COLOR_DEEP_GREEN);

        JLabel formulaLabel = new JLabel("Formula: (25.0 km / 15.0 km/L) × ₹105.00 = ₹175.00 total");
        formulaLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        formulaLabel.setForeground(COLOR_MUTED_TEXT);

        JLabel disclaimerLabel = new JLabel("⚠️ Note: Approximate estimate for student cost-sharing only.");
        disclaimerLabel.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        disclaimerLabel.setForeground(COLOR_MUTED_TEXT);

        resultBox.add(totalCostLabel);
        resultBox.add(Box.createVerticalStrut(4));
        resultBox.add(perPersonLabel);
        resultBox.add(Box.createVerticalStrut(4));
        resultBox.add(formulaLabel);
        resultBox.add(Box.createVerticalStrut(4));
        resultBox.add(disclaimerLabel);

        JPanel centerContainer = new JPanel();
        centerContainer.setLayout(new BoxLayout(centerContainer, BoxLayout.Y_AXIS));
        centerContainer.setOpaque(false);
        centerContainer.add(inputsCard);
        centerContainer.add(Box.createVerticalStrut(10));
        centerContainer.add(resultBox);
        panel.add(centerContainer, BorderLayout.CENTER);

        Runnable calculate = () -> {
            try {
                double dist = Double.parseDouble(distField.getText().trim());
                double mileage = Double.parseDouble(mileageField.getText().trim());
                double price = Double.parseDouble(fuelPriceField.getText().trim());
                int people = Integer.parseInt(peopleField.getText().trim());

                if (dist <= 0 || mileage <= 0 || price <= 0 || people <= 0) {
                    JOptionPane.showMessageDialog(dialog, "Please enter positive numbers greater than zero.", "Invalid Input", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                double totalCost = (dist / mileage) * price;
                double perPerson = totalCost / people;

                totalCostLabel.setText(String.format("Estimated Petrol Cost: ₹ %.2f", totalCost));
                perPersonLabel.setText(String.format("Cost Per Person (Estimate): ₹ %.2f", perPerson));
                formulaLabel.setText(String.format("Formula: (%.1f km / %.1f km/L) × ₹%.2f = ₹%.2f total", dist, mileage, price, totalCost));
            } catch (NumberFormatException nfe) {
                JOptionPane.showMessageDialog(dialog, "Please enter valid numeric values in all fields.", "Numeric Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        JButton calcBtn = createSecondaryButton("Calculate");
        calcBtn.addActionListener(e -> calculate.run());

        JButton closeBtn = createOutlineButton("Close");
        closeBtn.addActionListener(e -> dialog.dispose());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);
        actions.add(calcBtn);

        if (targetPriceField != null) {
            JButton applyBtn = createPrimaryButton("Apply to Ride Fare ($/₹)");
            applyBtn.addActionListener(e -> {
                try {
                    double dist = Double.parseDouble(distField.getText().trim());
                    double mileage = Double.parseDouble(mileageField.getText().trim());
                    double price = Double.parseDouble(fuelPriceField.getText().trim());
                    int people = Integer.parseInt(peopleField.getText().trim());
                    double perPerson = ((dist / mileage) * price) / people;
                    targetPriceField.setText(String.format("%.2f", perPerson));
                    dialog.dispose();
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dialog, "Calculate valid cost before applying.", "Error", JOptionPane.WARNING_MESSAGE);
                }
            });
            actions.add(applyBtn);
        }

        actions.add(closeBtn);
        panel.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }

    // =========================================================================
    // SCREEN 8: Profile Screen
    // =========================================================================
    private JPanel createProfilePanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(16, 16));
        contentPanel.setBackground(COLOR_CREAM);
        contentPanel.setBorder(new EmptyBorder(22, 28, 22, 28));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);

        JLabel title = new JLabel("Student Profile");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(COLOR_FOREST_GREEN);

        JLabel sub = new JLabel("Official student credentials and verification status");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        sub.setForeground(COLOR_MUTED_TEXT);

        titles.add(title);
        titles.add(Box.createVerticalStrut(2));
        titles.add(sub);

        JButton backBtn = createOutlineButton("← Back to Dashboard");
        backBtn.addActionListener(e -> showScreen(SCREEN_DASHBOARD));
        header.add(titles, BorderLayout.WEST);
        header.add(backBtn, BorderLayout.EAST);
        contentPanel.add(header, BorderLayout.NORTH);

        JPanel card = createCardPanel(480, 440);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));

        JLabel avatarBadge = new JLabel("NSSCE Student Profile");
        avatarBadge.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatarBadge.setForeground(COLOR_FOREST_GREEN);
        avatarBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        profileNameLabel = new JLabel("Name: ");
        profileIdLabel = new JLabel("Student ID: ");
        profileEmailLabel = new JLabel("College Email: ");
        profileRoleLabel = new JLabel("Role: Registered NSSCE Student");
        profileVerifyStatusLabel = new JLabel("Verification: Verified Student ✔");

        Font labelFont = new Font("Segoe UI", Font.PLAIN, 15);
        profileNameLabel.setFont(labelFont);
        profileNameLabel.setForeground(COLOR_MAIN_TEXT);
        profileIdLabel.setFont(labelFont);
        profileIdLabel.setForeground(COLOR_MAIN_TEXT);
        profileEmailLabel.setFont(labelFont);
        profileEmailLabel.setForeground(COLOR_MAIN_TEXT);
        profileRoleLabel.setFont(labelFont);
        profileRoleLabel.setForeground(COLOR_MAIN_TEXT);
        profileVerifyStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        profileVerifyStatusLabel.setForeground(COLOR_SUCCESS);

        JButton returnBtn = createPrimaryButton("Return to Dashboard");
        returnBtn.setMaximumSize(new Dimension(380, 40));
        returnBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        returnBtn.addActionListener(e -> showScreen(SCREEN_DASHBOARD));

        card.add(Box.createVerticalStrut(10));
        card.add(avatarBadge);
        card.add(Box.createVerticalStrut(22));
        card.add(createProfileRowPanel("Name:", profileNameLabel));
        card.add(Box.createVerticalStrut(10));
        card.add(createProfileRowPanel("Student ID:", profileIdLabel));
        card.add(Box.createVerticalStrut(10));
        card.add(createProfileRowPanel("Email:", profileEmailLabel));
        card.add(Box.createVerticalStrut(10));
        card.add(createProfileRowPanel("Role:", profileRoleLabel));
        card.add(Box.createVerticalStrut(10));
        card.add(createProfileRowPanel("Status:", profileVerifyStatusLabel));
        card.add(Box.createVerticalStrut(28));
        card.add(returnBtn);
        card.add(Box.createVerticalStrut(12));

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.add(card);

        contentPanel.add(centerWrapper, BorderLayout.CENTER);
        return createAuthenticatedLayout(SCREEN_PROFILE, contentPanel);
    }

    private JPanel createProfileRowPanel(String tag, JLabel label) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(420, 32));
        label.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        row.add(label, BorderLayout.CENTER);
        return row;
    }

    private void refreshProfile() {
        Student current = authService.getCurrentLoggedInStudent();
        if (current != null) {
            current.displayProfile();

            profileNameLabel.setText("Name: " + current.getName());
            profileIdLabel.setText("Student ID: " + current.getStudentId());
            profileEmailLabel.setText("College Email: " + current.getEmail());
            profileRoleLabel.setText("Role: Registered NSSCE Student");
            boolean isVerified = authService.isEmailVerified(current.getEmail());
            profileVerifyStatusLabel.setText("Verification: " + (isVerified ? "Verified Student ✔" : "Pending Verification ⏳"));
            profileVerifyStatusLabel.setForeground(isVerified ? COLOR_SUCCESS : COLOR_WARNING);
        }
    }

    // =========================================================================
    // Logout Action
    // =========================================================================
    private void handleLogout() {
        int opt = JOptionPane.showConfirmDialog(mainFrame, "Are you sure you want to log out?", "Confirm Logout", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            authService.logout();
            showScreen(SCREEN_LOGIN);
        }
    }

    // =========================================================================
    // UI Helpers & Reusable Design System Components
    // =========================================================================

    /**
     * Creates an ivory rounded card panel with subtle border.
     */
    private JPanel createCardPanel(int width, int height) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_IVORY_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.setColor(new Color(0xD6, 0xDE, 0xD2));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setPreferredSize(new Dimension(width, height));
        card.setMaximumSize(new Dimension(width, height));
        card.setBorder(new EmptyBorder(24, 28, 24, 28));
        return card;
    }

    /**
     * Creates a stat summary card displaying strictly live data metrics.
     */
    private JPanel createStatCard(String title, JLabel valueLabel, String subtitle) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(COLOR_IVORY_CARD);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(COLOR_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 18, 14, 18));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(COLOR_MUTED_TEXT);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        valueLabel.setForeground(COLOR_FOREST_GREEN);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(COLOR_MUTED_TEXT);
        subLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(4));
        card.add(valueLabel);
        card.add(Box.createVerticalStrut(3));
        card.add(subLbl);

        return card;
    }

    /**
     * Creates a styled text field with high-contrast font and active focus highlight ring.
     */
    private JTextField createStyledTextField() {
        JTextField field = new JTextField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(COLOR_MAIN_TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(COLOR_FOREST_GREEN);
        field.setOpaque(true);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB8, 0xC6, 0xB5), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_FOREST_GREEN, 2),
                        new EmptyBorder(7, 11, 7, 11)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xB8, 0xC6, 0xB5), 1),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        return field;
    }

    /**
     * Creates a styled password field matching the text fields.
     */
    private JPasswordField createStyledPasswordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(COLOR_MAIN_TEXT);
        field.setBackground(Color.WHITE);
        field.setCaretColor(COLOR_FOREST_GREEN);
        field.setOpaque(true);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB8, 0xC6, 0xB5), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(COLOR_FOREST_GREEN, 2),
                        new EmptyBorder(7, 11, 7, 11)
                ));
            }
            @Override
            public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(0xB8, 0xC6, 0xB5), 1),
                        new EmptyBorder(8, 12, 8, 12)
                ));
            }
        });
        return field;
    }

    private void addFormField(JPanel container, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(COLOR_MAIN_TEXT);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(420, 38));

        container.add(label);
        container.add(Box.createVerticalStrut(4));
        container.add(field);
        container.add(Box.createVerticalStrut(9));
    }

    /**
     * Forest-green primary button with high-contrast white text and custom painted rounded background.
     * Guaranteed to never be overridden by Windows LookAndFeel default button styling.
     */
    private JButton createPrimaryButton(String text) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(COLOR_FOREST_GREEN);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_FOREST_GREEN, 1),
                new EmptyBorder(8, 16, 8, 16)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(COLOR_HOVER_GREEN);
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(COLOR_FOREST_GREEN);
                btn.repaint();
            }
        });
        return btn;
    }

    /**
     * High-contrast Sage/Gold-accented secondary button with dark deep-forest text.
     */
    private JButton createSecondaryButton(String text) {
        Color normalBg = COLOR_SAGE_ACCENT; // #CFE0CC
        Color hoverBg = new Color(0xBE, 0xD3, 0xBB);
        Color borderCol = new Color(0x9E, 0xB6, 0x9B);
        Color textCol = COLOR_DEEP_GREEN; // #102F27

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(textCol);
        btn.setBackground(normalBg);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderCol, 1),
                new EmptyBorder(8, 14, 8, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalBg);
                btn.repaint();
            }
        });
        return btn;
    }

    /**
     * Clean outline button with warm cream background and dark forest-green text.
     */
    private JButton createOutlineButton(String text) {
        Color normalBg = new Color(0xF7, 0xF3, 0xE8);
        Color hoverBg = new Color(0xEB, 0xE5, 0xD4);
        Color borderCol = new Color(0xA8, 0xBD, 0xA5);
        Color textCol = COLOR_FOREST_GREEN;

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(textCol);
        btn.setBackground(normalBg);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderCol, 1),
                new EmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalBg);
                btn.repaint();
            }
        });
        return btn;
    }

    /**
     * High-contrast Crimson Red danger button with pure white text.
     */
    private JButton createDangerButton(String text) {
        Color normalBg = COLOR_DANGER; // #991B1B
        Color hoverBg = COLOR_DANGER_HOVER; // #B91C1C
        Color textCol = Color.WHITE;

        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(textCol);
        btn.setBackground(normalBg);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xB8, 0x30, 0x30), 1),
                new EmptyBorder(7, 14, 7, 14)
        ));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                btn.setBackground(hoverBg);
                btn.repaint();
            }
            @Override
            public void mouseExited(MouseEvent e) {
                btn.setBackground(normalBg);
                btn.repaint();
            }
        });
        return btn;
    }

    /**
     * Styles JTable with forest-green header, alternating ivory rows, and sage selection.
     */
    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setRowHeight(36);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setGridColor(COLOR_BORDER);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setSelectionBackground(new Color(0xE0, 0xEC, 0xDE));
        table.setSelectionForeground(COLOR_DEEP_GREEN);

        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSel, hasFocus, row, col);
                lbl.setBackground(COLOR_FOREST_GREEN);
                lbl.setForeground(COLOR_CREAM);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        header.setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer cellRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean isSelected, boolean hasFocus, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, isSelected, hasFocus, row, col);
                if (!isSelected) {
                    lbl.setBackground(row % 2 == 0 ? COLOR_IVORY_CARD : Color.WHITE);
                    lbl.setForeground(COLOR_MAIN_TEXT);
                } else {
                    lbl.setBackground(new Color(0xE0, 0xEC, 0xDE));
                    lbl.setForeground(COLOR_DEEP_GREEN);
                }
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                lbl.setOpaque(true);
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, cellRenderer);
    }
}
