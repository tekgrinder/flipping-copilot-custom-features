package com.flippingcopilot.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SuggestionPreferences {

    private boolean f2pOnlyMode = false;
    private boolean sellOnlyMode = false;
    private boolean whitelistMode = false;  // false = blacklist mode, true = whitelist mode
    private List<Integer> blockedItemIds = new ArrayList<>();
    private List<Integer> whitelistedItemIds = new ArrayList<>();  // Empty list = all items blocked by default
}
