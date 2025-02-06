package com.flippingcopilot.ui;

import com.flippingcopilot.model.SuggestionPreferences;
import com.flippingcopilot.model.SuggestionPreferencesManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.ui.ColorScheme;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

@Slf4j
@Singleton
public class BlacklistDropdownPanel extends JPanel {
    private final SuggestionPreferencesManager preferencesManager;
    private final JTextField displayField;
    private final JWindow dropdownWindow;
    private final JPanel resultsPanel;
    private final JScrollPane scrollPane;
    private final JTextField searchField;
    private final ClientThread clientThread;
    private final JToggleButton modeToggleButton;

    @Inject
    public BlacklistDropdownPanel(SuggestionPreferencesManager preferencesManager, ClientThread clientThread) {
        super();
        this.preferencesManager = preferencesManager;
        this.clientThread = clientThread;

        setLayout(new BorderLayout());

        // Create main display field with placeholder
        displayField = new JTextField("Search an item...");
        displayField.setEditable(true);
        displayField.setPreferredSize(new Dimension(12, displayField.getPreferredSize().height));
        displayField.setForeground(Color.GRAY);

        // Setup dropdown components first
        dropdownWindow = new JWindow();
        searchField = new JTextField();

        // Create mode toggle button
        modeToggleButton = new JToggleButton("Blacklist");
        modeToggleButton.setSelected(preferencesManager.isWhitelistMode());
        updateModeToggleButton();
        modeToggleButton.addActionListener(e -> {
            preferencesManager.setWhitelistMode(modeToggleButton.isSelected());
            updateModeToggleButton();
            updateDropdown(searchField.getText());
        });

        // Create label panel with mode toggle
        JPanel labelPanel = new JPanel(new BorderLayout(5, 0));
        JLabel label = new JLabel("Filter:");
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        label.setOpaque(true);
        label.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        labelPanel.add(label, BorderLayout.WEST);
        labelPanel.add(modeToggleButton, BorderLayout.CENTER);
        labelPanel.setOpaque(true);
        labelPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create search field panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(displayField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        searchPanel.setOpaque(true);
        searchPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create reset button
        JButton resetButton = new JButton("Reset to Default");
        resetButton.setToolTipText("Reset to default: Whitelist - block all items, Blacklist - allow all items");
        resetButton.addActionListener(e -> {
            preferencesManager.resetCurrentList();
            updateDropdown(searchField.getText());
        });
        JPanel resetPanel = new JPanel(new BorderLayout());
        resetPanel.add(resetButton, BorderLayout.CENTER);
        resetPanel.setOpaque(true);
        resetPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create vertical panel for toggle, search, and reset
        JPanel verticalPanel = new JPanel();
        verticalPanel.setLayout(new BoxLayout(verticalPanel, BoxLayout.Y_AXIS));
        verticalPanel.add(labelPanel);
        verticalPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add 5 pixels vertical spacing
        verticalPanel.add(searchPanel);
        verticalPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Add 5 pixels vertical spacing
        verticalPanel.add(resetPanel);
        verticalPanel.setOpaque(true);
        verticalPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);

        // Create container panel
        JPanel containerPanel = new JPanel(new BorderLayout());
        containerPanel.add(verticalPanel, BorderLayout.CENTER);
        containerPanel.setOpaque(true);
        containerPanel.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        add(containerPanel, BorderLayout.CENTER);
        resultsPanel = new JPanel();
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));

        scrollPane = new JScrollPane(resultsPanel);
        scrollPane.setPreferredSize(new Dimension(300, 400));

        // Create dropdown content panel
        JPanel dropdownContent = new JPanel(new BorderLayout());
        dropdownContent.add(searchField, BorderLayout.NORTH);
        dropdownContent.add(scrollPane, BorderLayout.CENTER);
        dropdownContent.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        dropdownWindow.add(dropdownContent);

        setupListeners();
    }

    private void updateDropdown(String searchText) {
        clientThread.invoke(() -> {
            // Get fresh data
            List<Pair<Integer, String>> searchResults = preferencesManager.search(searchText);
            SuggestionPreferences preferences = preferencesManager.getPreferences();
            Set<Integer> filteredItems;
            if (preferences.isWhitelistMode()) {
                // In whitelist mode, pass the whitelist (items that are allowed)
                filteredItems = new HashSet<>(preferences.getWhitelistedItemIds());
            } else {
                // In blacklist mode, pass the blacklist (items that are blocked)
                filteredItems = new HashSet<>(preferences.getBlockedItemIds());
            }

            SwingUtilities.invokeLater(() -> {
                // Update results panel
                resultsPanel.removeAll();
                for (Pair<Integer, String> item : searchResults) {
                    resultsPanel.add(createItemPanel(item, filteredItems));
                }
                // Calculate dimensions
                Point location = getLocationOnScreen();
                int searchHeight = searchField.getPreferredSize().height;
                int scrollBarHeight = scrollPane.getHorizontalScrollBar().getPreferredSize().height;
                int contentHeight = Arrays.stream(resultsPanel.getComponents())
                        .mapToInt(comp -> comp.getPreferredSize().height)
                        .sum();

                int totalHeight = Math.min(
                        contentHeight + searchHeight + scrollBarHeight + 12, // 12 for border and padding
                        400 // Maximum height
                );

                // Update window
                dropdownWindow.setLocation(location.x, location.y + getHeight());
                dropdownWindow.setSize(getWidth(), totalHeight);
                dropdownWindow.setVisible(true);

                // Update UI
                resultsPanel.revalidate();
                resultsPanel.repaint();
                searchField.setText(searchText);
            });
        });
    }

    private void setupListeners() {

        displayField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                log.debug("mouse clicked");
                updateDropdown(displayField.getText());
            }
        });

        displayField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (displayField.getText().equals("Search an item...")) {
                    displayField.setText("");
                    updateDropdown(displayField.getText());
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                log.debug("focus lost");
                displayField.setText("Search an item...");
                displayField.setForeground(Color.GRAY);
                dropdownWindow.setVisible(false);
            }
        });

        // Display field key listener
        displayField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    updateDropdown(displayField.getText());
                    e.consume();
                }
            }
        });
    }

    public void updateModeToggleButton() {
        boolean isWhitelistMode = preferencesManager.isWhitelistMode();
        modeToggleButton.setSelected(isWhitelistMode);
        modeToggleButton.setText(isWhitelistMode ? "Whitelist" : "Blacklist");
        modeToggleButton.setToolTipText(isWhitelistMode ? 
            "Only show suggestions for whitelisted items" : 
            "Show suggestions for all items except blacklisted ones");
    }

    private JPanel createItemPanel(Pair<Integer, String> item, Set<Integer> filteredItems) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));

        JLabel nameLabel = new JLabel(item.getValue());
        panel.add(nameLabel, BorderLayout.CENTER);

        boolean isFiltered = filteredItems.contains(item.getKey());
        boolean isWhitelistMode = preferencesManager.isWhitelistMode();
        
        // For whitelist: check = whitelisted, X = not whitelisted
        // For blacklist: check = not blacklisted, X = blacklisted
        boolean showCheck = isWhitelistMode ? isFiltered : !isFiltered;
        JButton toggleButton = new JButton(showCheck ? BlacklistIcons.createTickIcon() : BlacklistIcons.createXIcon());
        toggleButton.setBorderPainted(false);
        toggleButton.setContentAreaFilled(false);
        toggleButton.setPreferredSize(new Dimension(16, 16));

        Runnable onClick = () -> {
            preferencesManager.toggleItem(item.getKey());
            boolean newState = preferencesManager.isItemFiltered(item.getKey());
            boolean showCheckNew = isWhitelistMode ? newState : !newState;
            toggleButton.setIcon(showCheckNew ? BlacklistIcons.createTickIcon() : BlacklistIcons.createXIcon());
            if (newState) {
                filteredItems.add(item.getKey());
            } else {
                filteredItems.remove(item.getKey());
            }
            panel.revalidate();
            panel.repaint();
        };

        toggleButton.addActionListener(e -> onClick.run());
        panel.add(toggleButton, BorderLayout.EAST);

        panel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent ee) {
                onClick.run();
            }
        });
        return panel;
    }
}