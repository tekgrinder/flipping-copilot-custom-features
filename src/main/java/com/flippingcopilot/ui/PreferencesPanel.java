package com.flippingcopilot.ui;

import com.flippingcopilot.controller.FlippingCopilotConfig;
import com.flippingcopilot.model.OsrsLoginManager;
import com.flippingcopilot.model.SuggestionPreferencesManager;
import com.flippingcopilot.model.SuggestionManager;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.SwingUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class PreferencesPanel extends JPanel {

    public static String LOGIN_TO_MANAGE_SETTINGS = "Log in to manage settings";

    private final Client client;
    private final OsrsLoginManager osrsLoginManager;
    private final SuggestionPreferencesManager preferencesManager;
    private final ItemManager itemManager;
    private final ClientThread clientThread;
    private final JPanel sellOnlyButton;
    private final PreferencesToggleButton sellOnlyModeToggleButton;
    private final JPanel f2pOnlyButton;
    private final PreferencesToggleButton f2pOnlyModeToggleButton;
    private final BlacklistDropdownPanel blacklistDropdownPanel;
    private final JPanel buttonPanel;
    private final JLabel messageText = new JLabel();
    private final FlippingCopilotConfig config;
    private final JComboBox<String> filterFileComboBox;
    private final DefaultComboBoxModel<String> filterFileModel;

    @Inject
    public PreferencesPanel(
            OsrsLoginManager osrsLoginManager,
            SuggestionManager suggestionManager,
            SuggestionPreferencesManager suggestionPreferencesManager,
            Client client,
            ItemManager itemManager,
            ClientThread clientThread,
            SuggestionPreferencesManager preferencesManager,
            BlacklistDropdownPanel blocklistDropdownPanel,
            FlippingCopilotConfig config) {
        super();
        this.osrsLoginManager = osrsLoginManager;
        this.client = client;
        this.itemManager = itemManager;
        this.clientThread = clientThread;
        this.preferencesManager = preferencesManager;
        this.blacklistDropdownPanel = blocklistDropdownPanel;
        this.config = config;
        this.filterFileModel = new DefaultComboBoxModel<>();
        this.filterFileComboBox = new JComboBox<>(filterFileModel);
        
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBounds(0, 0, 225, 400);

        // Create a panel for the content
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // Create scroll pane
        JScrollPane scrollPane = new JScrollPane(contentPanel);
        scrollPane.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        JLabel preferencesTitle = new JLabel("Suggestion Settings");
        preferencesTitle.setForeground(Color.WHITE);
        preferencesTitle.setFont(preferencesTitle.getFont().deriveFont(Font.BOLD));
        preferencesTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        preferencesTitle.setMinimumSize(new Dimension(225, preferencesTitle.getPreferredSize().height));
        preferencesTitle.setMaximumSize(new Dimension(225, preferencesTitle.getPreferredSize().height));
        preferencesTitle.setHorizontalAlignment(SwingConstants.CENTER);
        contentPanel.add(preferencesTitle);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        sellOnlyModeToggleButton = new PreferencesToggleButton();
        sellOnlyButton = new JPanel();
        sellOnlyButton.setLayout(new BorderLayout());
        sellOnlyButton.setOpaque(false);
        contentPanel.add(sellOnlyButton);
        JLabel buttonText = new JLabel("Sell-only mode");
        sellOnlyButton.add(buttonText, BorderLayout.LINE_START);
        sellOnlyButton.add(sellOnlyModeToggleButton, BorderLayout.LINE_END);
        sellOnlyModeToggleButton.addItemListener(i ->
        {
            suggestionPreferencesManager.setSellOnlyMode(sellOnlyModeToggleButton.isSelected());
            suggestionManager.setSuggestionNeeded(true);
        });
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        f2pOnlyModeToggleButton = new PreferencesToggleButton();
        f2pOnlyButton = new JPanel();
        f2pOnlyButton.setLayout(new BorderLayout());
        f2pOnlyButton.setOpaque(false);
        contentPanel.add(f2pOnlyButton);
        JLabel f2pOnlyButtonText = new JLabel("F2P-only mode");
        f2pOnlyButton.add(f2pOnlyButtonText, BorderLayout.LINE_START);
        f2pOnlyButton.add(f2pOnlyModeToggleButton, BorderLayout.LINE_END);
        f2pOnlyModeToggleButton.addItemListener(i ->
        {
            suggestionPreferencesManager.setF2pOnlyMode(f2pOnlyModeToggleButton.isSelected());
            suggestionManager.setSuggestionNeeded(true);
        });
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        contentPanel.add(this.blacklistDropdownPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 5)));

        // Add filter file dropdown
        JPanel dropdownPanel = new JPanel(new BorderLayout(5, 0));
        dropdownPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        dropdownPanel.setOpaque(true);
        dropdownPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        filterFileComboBox.setPreferredSize(new Dimension(200, 25));
        filterFileComboBox.addActionListener(e -> {
            // Ignore events during model updates
            if (filterFileComboBox.getSelectedItem() == null) {
                return;
            }
            String selectedFile = (String) filterFileComboBox.getSelectedItem();
            if (!selectedFile.equals("Select a filter file...")) {
                importFilterFile(selectedFile);
                SwingUtilities.invokeLater(() -> {
                    filterFileModel.setSelectedItem("Select a filter file...");
                });
            }
        });
        updateFilterFileList();
        dropdownPanel.add(filterFileComboBox, BorderLayout.CENTER);
        contentPanel.add(dropdownPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        
        // Add export/import buttons panel
        buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
        buttonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        
        // Create invert button panel
        JPanel invertButtonPanel = new JPanel();
        invertButtonPanel.setLayout(new BoxLayout(invertButtonPanel, BoxLayout.X_AXIS));
        invertButtonPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        JButton invertButton = new JButton("Invert List");
        invertButton.addActionListener(e -> invertFilteredList());
        invertButtonPanel.add(invertButton);
        contentPanel.add(invertButtonPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // Create export/import buttons
        JButton exportButton = new JButton("Export List");
        exportButton.addActionListener(e -> {
            exportPreferences();
            updateFilterFileList(); // Update dropdown after export
        });
        
        JButton importButton = new JButton("Import List");
        importButton.addActionListener(e -> {
            importPreferences();
            updateFilterFileList(); // Update dropdown after import
        });

        buttonPanel.add(exportButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        buttonPanel.add(importButton);
        
        contentPanel.add(buttonPanel);
        contentPanel.add(Box.createRigidArea(new Dimension(0, 15)));
        contentPanel.add(messageText);
        messageText.setText(LOGIN_TO_MANAGE_SETTINGS);
        messageText.setVisible(false);
        messageText.setAlignmentX(Component.CENTER_ALIGNMENT);
        messageText.setHorizontalAlignment(SwingConstants.CENTER);
    }

    private void updateFilterFileList() {
        SwingUtilities.invokeLater(() -> {
            String currentSelection = (String) filterFileComboBox.getSelectedItem();
            filterFileModel.removeAllElements();
            filterFileModel.addElement("Select a filter file...");
            
            try {
                String dirPath = config.filterDirectory();
                if (dirPath != null && !dirPath.isEmpty()) {
                    Path directory = Paths.get(dirPath);
                    if (Files.exists(directory) && Files.isDirectory(directory)) {
                        Files.list(directory)
                            .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                            .map(path -> path.getFileName().toString())
                            .sorted()
                            .forEach(filterFileModel::addElement);
                    }
                }
            } catch (IOException e) {
                log.error("Error scanning filter directory", e);
            }

            // Restore selection or default to first item
            if (currentSelection != null && filterFileModel.getIndexOf(currentSelection) >= 0) {
                filterFileModel.setSelectedItem(currentSelection);
            } else {
                filterFileModel.setSelectedItem("Select a filter file...");
            }
        });
    }

    private void importFilterFile(String fileName) {
        if (fileName == null || fileName.equals("Select a filter file...")) {
            return;
        }

        if (osrsLoginManager.getPlayerDisplayName() == null || client.getGameState() != GameState.LOGGED_IN) {
            JOptionPane.showMessageDialog(this,
                "You must be logged in to import preferences.",
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        File file = new File(config.filterDirectory(), fileName);
        if (!file.exists()) {
            JOptionPane.showMessageDialog(this,
                "Filter file not found: " + fileName,
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            // Read mode from header
            String modeHeader = reader.readLine();
            if (!modeHeader.startsWith("# Mode:")) {
                throw new IOException("Invalid file format: missing mode header");
            }
            
            boolean isWhitelistMode = modeHeader.toLowerCase().contains("whitelist");
            if (isWhitelistMode != preferencesManager.isWhitelistMode()) {
                int choice = JOptionPane.showConfirmDialog(this,
                    "The imported list uses a different mode (whitelist/blacklist) than your current settings.\n" +
                    "Would you like to switch modes to match the imported list?",
                    "Mode Mismatch",
                    JOptionPane.YES_NO_CANCEL_OPTION);
                    
                if (choice == JOptionPane.CANCEL_OPTION) {
                    return;
                } else if (choice == JOptionPane.YES_OPTION) {
                    preferencesManager.setWhitelistMode(isWhitelistMode);
                    blacklistDropdownPanel.updateModeToggleButton();
                }
            }
            
            // Skip CSV header
            reader.readLine();
            
            // Clear current list
            preferencesManager.resetCurrentList();
            
            // Read and process items
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 3) {
                    int itemId = Integer.parseInt(parts[0]);
                    boolean isFiltered = Boolean.parseBoolean(parts[2]);
                    if (isFiltered) {
                        preferencesManager.toggleItem(itemId);
                    }
                }
            }
            
            JOptionPane.showMessageDialog(this,
                "Filter list imported successfully!",
                "Import Complete",
                JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                "Error importing filter list: " + e.getMessage(),
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    // For testing purposes
    protected void exportToFile(File file) throws IOException {
        // Get the current list based on mode
        boolean isWhitelistMode = preferencesManager.isWhitelistMode();
        java.util.List<Integer> tempItems = isWhitelistMode ? 
            preferencesManager.getPreferences().getWhitelistedItemIds() : 
            preferencesManager.getPreferences().getBlockedItemIds();

        // Create a final copy of the list
        final java.util.List<Integer> filteredItems = tempItems != null ? 
            new ArrayList<>(tempItems) : new ArrayList<>();

        // Get item names on client thread
        Map<Integer, String> itemNames = new HashMap<>();
        CountDownLatch latch = new CountDownLatch(1);

        clientThread.invoke(() -> {
            try {
                for (Integer itemId : filteredItems) {
                    try {
                        String name = itemManager.getItemComposition(itemId).getName();
                        itemNames.put(itemId, name);
                    } catch (Exception e) {
                        log.warn("Error getting item name for ID: " + itemId, e);
                        itemNames.put(itemId, "Unknown Item " + itemId);
                    }
                }
            } finally {
                latch.countDown();
            }
        });

        try {
            // Wait for item names to be collected (timeout after 5 seconds)
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IOException("Timeout while getting item names");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while getting item names", e);
        }

        // Write to file
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            // Write header with mode information
            writer.write("# Mode: " + (isWhitelistMode ? "whitelist" : "blacklist"));
            writer.newLine();
            writer.write("item_id,name,is_filtered");
            writer.newLine();
            
            // Write items
            for (Integer itemId : filteredItems) {
                String itemName = itemNames.getOrDefault(itemId, "Unknown Item " + itemId);
                writer.write(String.format("%d,%s,true", itemId, itemName));
                writer.newLine();
            }
        }
    }

    private void exportPreferences() {
        if (osrsLoginManager.getPlayerDisplayName() == null || client.getGameState() != GameState.LOGGED_IN) {
            JOptionPane.showMessageDialog(this,
                "You must be logged in to export preferences.",
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(config.filterDirectory());
        fileChooser.setDialogTitle("Export Filter List");
        
        // Set default filename with timestamp
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String defaultFileName = String.format("flipping_copilot_%s_%s.csv",
            preferencesManager.isWhitelistMode() ? "whitelist" : "blacklist",
            dateFormat.format(new Date()));
        fileChooser.setSelectedFile(new File(defaultFileName));
        
        // Only allow CSV files
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getParentFile(), file.getName() + ".csv");
            }
            
            try {
                exportToFile(file);
                JOptionPane.showMessageDialog(this,
                    "Filter list exported successfully!",
                    "Export Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                    "Error exporting filter list: " + e.getMessage(),
                    "Export Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importPreferences() {
        if (osrsLoginManager.getPlayerDisplayName() == null || client.getGameState() != GameState.LOGGED_IN) {
            JOptionPane.showMessageDialog(this,
                "You must be logged in to import preferences.",
                "Import Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser(config.filterDirectory());
        fileChooser.setDialogTitle("Import Filter List");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("CSV Files", "csv");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(fileChooser.getSelectedFile()), StandardCharsets.UTF_8))) {
                // Read mode from header
                String modeHeader = reader.readLine();
                if (!modeHeader.startsWith("# Mode:")) {
                    throw new IOException("Invalid file format: missing mode header");
                }
                
                boolean isWhitelistMode = modeHeader.toLowerCase().contains("whitelist");
                if (isWhitelistMode != preferencesManager.isWhitelistMode()) {
                    int choice = JOptionPane.showConfirmDialog(this,
                        "The imported list uses a different mode (whitelist/blacklist) than your current settings.\n" +
                        "Would you like to switch modes to match the imported list?",
                        "Mode Mismatch",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                        
                    if (choice == JOptionPane.CANCEL_OPTION) {
                        return;
                    } else if (choice == JOptionPane.YES_OPTION) {
                        preferencesManager.setWhitelistMode(isWhitelistMode);
                        blacklistDropdownPanel.updateModeToggleButton();
                    }
                }
                
                // Skip CSV header
                reader.readLine();
                
                // Clear current list
                preferencesManager.resetCurrentList();
                
                // Read and process items
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length >= 3) {
                        int itemId = Integer.parseInt(parts[0]);
                        boolean isFiltered = Boolean.parseBoolean(parts[2]);
                        if (isFiltered) {
                            preferencesManager.toggleItem(itemId);
                        }
                    }
                }
                
                JOptionPane.showMessageDialog(this,
                    "Filter list imported successfully!",
                    "Import Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                    "Error importing filter list: " + e.getMessage(),
                    "Import Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refresh() {
        updateFilterFileList();
    }

    private void invertFilteredList() {
        if (osrsLoginManager.getPlayerDisplayName() == null || client.getGameState() != GameState.LOGGED_IN) {
            JOptionPane.showMessageDialog(this,
                "You must be logged in to invert the list.",
                "Invert Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Get all tradeable items
        java.util.List<Integer> allTradeableItems = new ArrayList<>();
        clientThread.invoke(() -> {
            for (int i = 0; i < client.getItemCount(); i++) {
                try {
                    if (itemManager.getItemComposition(i).isTradeable() && 
                        itemManager.getItemComposition(i).getNote() == -1) {
                        allTradeableItems.add(i);
                    }
                } catch (Exception e) {
                    // Skip items that can't be loaded
                }
            }

            // Get current filtered items
            java.util.List<Integer> currentFiltered = preferencesManager.isWhitelistMode() ?
                preferencesManager.getPreferences().getWhitelistedItemIds() :
                preferencesManager.getPreferences().getBlockedItemIds();

            // Create a set of currently filtered items for faster lookup
            java.util.Set<Integer> currentFilteredSet = new java.util.HashSet<>(currentFiltered);

            // Clear current list
            preferencesManager.resetCurrentList();

            // Add all items that were not in the original list
            for (Integer itemId : allTradeableItems) {
                if (!currentFilteredSet.contains(itemId)) {
                    preferencesManager.toggleItem(itemId);
                }
            }
        });
    }
}