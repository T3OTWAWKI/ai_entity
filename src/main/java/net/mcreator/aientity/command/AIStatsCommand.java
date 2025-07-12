package net.mcreator.aientity.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.mcreator.aientity.learning.ReinforcementLearningSystem;

public class AIStatsCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("aistats")
                .requires(source -> source.hasPermission(2))
                .executes(context -> showStats(context.getSource()))
                .then(Commands.literal("reset")
                        .executes(context -> resetStats(context.getSource())))
                .then(Commands.literal("exploration")
                        .then(Commands.argument("rate", StringArgumentType.string())
                                .executes(context -> setExplorationRate(context.getSource(), 
                                        StringArgumentType.getString(context, "rate")))));
        
        dispatcher.register(command);
    }
    
    private static int showStats(CommandSourceStack source) {
        ServerLevel serverLevel = source.getLevel();
        if (serverLevel != null) {
            ReinforcementLearningSystem rlSystem = ReinforcementLearningSystem.get(serverLevel);
            
            source.sendSuccess(() -> Component.literal("§6=== AI Entity Learning Stats ==="), false);
            source.sendSuccess(() -> Component.literal("§eTotal Encounters: §f" + rlSystem.getTotalEncounters()), false);
            source.sendSuccess(() -> Component.literal("§eSuccessful Kills: §f" + rlSystem.getSuccessfulKills()), false);
            source.sendSuccess(() -> Component.literal("§ePlayer Escapes: §f" + rlSystem.getPlayerEscapes()), false);
            source.sendSuccess(() -> Component.literal("§eSuccess Rate: §f" + String.format("%.2f%%", rlSystem.getSuccessRate() * 100)), false);
            source.sendSuccess(() -> Component.literal("§eExploration Rate: §f" + String.format("%.4f", rlSystem.getExplorationRate())), false);
            source.sendSuccess(() -> Component.literal("§eQ-Table Size: §f" + rlSystem.getQTableSize() + " states"), false);
            source.sendSuccess(() -> Component.literal("§eTotal State-Actions: §f" + rlSystem.getTotalStateActionPairs()), false);
            
            // Show learning progress
            double successRate = rlSystem.getSuccessRate();
            String progressBar = createProgressBar(successRate);
            source.sendSuccess(() -> Component.literal("§eLearning Progress: §f" + progressBar), false);
            
            // Show AI behavior insights
            if (rlSystem.getTotalEncounters() > 3) {
                String insight = getAIInsight(rlSystem);
                source.sendSuccess(() -> Component.literal("§eAI Insight: §f" + insight), false);
                
                String strategy = rlSystem.getCurrentStrategyAssessment();
                source.sendSuccess(() -> Component.literal("§eStrategy: §f" + strategy), false);
                
                String bestAction = rlSystem.getMostSuccessfulAction();
                source.sendSuccess(() -> Component.literal("§eBest Action: §f" + bestAction), false);
            }
            
            return 1;
        }
        
        source.sendFailure(Component.literal("§cCould not access AI learning system"));
        return 0;
    }
    
    private static int resetStats(CommandSourceStack source) {
        ServerLevel serverLevel = source.getLevel();
        if (serverLevel != null) {
            ReinforcementLearningSystem rlSystem = ReinforcementLearningSystem.get(serverLevel);
            rlSystem.resetLearning();
            
            source.sendSuccess(() -> Component.literal("§aAI learning stats have been reset!"), false);
            return 1;
        }
        
        source.sendFailure(Component.literal("§cCould not reset AI learning system"));
        return 0;
    }
    
    private static int setExplorationRate(CommandSourceStack source, String rateStr) {
        try {
            double rate = Double.parseDouble(rateStr);
            if (rate < 0.0 || rate > 1.0) {
                source.sendFailure(Component.literal("§cExploration rate must be between 0.0 and 1.0"));
                return 0;
            }
            
            ServerLevel serverLevel = source.getLevel();
            if (serverLevel != null) {
                ReinforcementLearningSystem rlSystem = ReinforcementLearningSystem.get(serverLevel);
                rlSystem.setExplorationRate(rate);
                
                source.sendSuccess(() -> Component.literal("§aExploration rate set to: §f" + rate), false);
                return 1;
            }
            
        } catch (NumberFormatException e) {
            source.sendFailure(Component.literal("§cInvalid number format"));
        }
        
        return 0;
    }
    
    private static String createProgressBar(double progress) {
        int barLength = 20;
        int filledLength = (int) (progress * barLength);
        
        StringBuilder bar = new StringBuilder("§a");
        for (int i = 0; i < filledLength; i++) {
            bar.append("█");
        }
        bar.append("§7");
        for (int i = filledLength; i < barLength; i++) {
            bar.append("█");
        }
        bar.append("§f (").append(String.format("%.1f%%", progress * 100)).append(")");
        
        return bar.toString();
    }
    
    private static String getAIInsight(ReinforcementLearningSystem rlSystem) {
        double successRate = rlSystem.getSuccessRate();
        double explorationRate = rlSystem.getExplorationRate();
        int encounters = rlSystem.getTotalEncounters();
        
        if (successRate > 0.8) {
            return "§aHighly effective - AI has mastered hunting strategies";
        } else if (successRate > 0.6) {
            return "§eImproving - AI is learning effective patterns";
        } else if (successRate > 0.4) {
            return "§6Learning - AI is still adapting to player behavior";
        } else if (encounters < 5) {
            return "§7Inexperienced - AI needs more encounters to learn";
        } else {
            return "§cStruggling - AI may need strategy adjustments";
        }
    }
}