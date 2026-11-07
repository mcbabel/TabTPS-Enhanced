/*
 * This file is part of TabTPS, licensed under the MIT License.
 *
 * Copyright (c) 2020-2024 Jason Penilla
 * Copyright (c) 2026 MCbabel (Forge support)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package xyz.jpenilla.tabtps.forge.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.commands.CommandSourceStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.SenderMapperHolder;
import org.incendo.cloud.brigadier.BrigadierManagerHolder;
import org.incendo.cloud.brigadier.CloudBrigadierCommand;
import org.incendo.cloud.brigadier.CloudBrigadierManager;
import org.incendo.cloud.brigadier.suggestion.TooltipSuggestion;
import org.incendo.cloud.component.CommandComponent;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.internal.CommandRegistrationHandler;
import org.incendo.cloud.meta.CommandMeta;
import org.incendo.cloud.meta.SimpleCommandMeta;
import org.incendo.cloud.suggestion.SuggestionFactory;
import xyz.jpenilla.tabtps.common.command.Commander;

@DefaultQualifier(NonNull.class)
public final class ForgeCommandManager extends CommandManager<Commander>
  implements BrigadierManagerHolder<Commander, CommandSourceStack>, SenderMapperHolder<CommandSourceStack, Commander> {
  private final Registration registration;
  private final SenderMapper<CommandSourceStack, Commander> senderMapper;
  private final SuggestionFactory<Commander, ? extends TooltipSuggestion> suggestionFactory;
  private final CloudBrigadierManager<Commander, CommandSourceStack> brigadierManager;

  public ForgeCommandManager(final SenderMapper<CommandSourceStack, Commander> senderMapper) {
    this(senderMapper, new Registration());
  }

  @SuppressWarnings("this-escape")
  private ForgeCommandManager(final SenderMapper<CommandSourceStack, Commander> senderMapper, final Registration registration) {
    super(ExecutionCoordinator.simpleCoordinator(), registration);
    this.registration = registration;
    this.senderMapper = senderMapper;
    this.suggestionFactory = super.suggestionFactory().mapped(TooltipSuggestion::tooltipSuggestion);
    this.brigadierManager = new CloudBrigadierManager<>(this, senderMapper);
  }

  public void registerCommands(final CommandDispatcher<CommandSourceStack> dispatcher) {
    this.lockRegistration();
    final RootCommandNode<CommandSourceStack> root = dispatcher.getRoot();
    for (final Command<Commander> command : this.registration.commands) {
      final CommandComponent<Commander> first = command.rootComponent();
      final LiteralCommandNode<CommandSourceStack> node = this.brigadierManager.literalBrigadierNodeFactory().createNode(
        first.name(),
        command,
        new CloudBrigadierCommand<>(this, this.brigadierManager)
      );
      root.addChild(node);
      for (final String alias : first.alternativeAliases()) {
        root.addChild(redirect(alias, node));
      }
    }
  }

  private static LiteralCommandNode<CommandSourceStack> redirect(final String alias, final CommandNode<CommandSourceStack> destination) {
    final LiteralArgumentBuilder<CommandSourceStack> builder = LiteralArgumentBuilder.<CommandSourceStack>literal(alias)
      .requires(destination.getRequirement())
      .forward(destination.getRedirect(), destination.getRedirectModifier(), destination.isFork())
      .executes(destination.getCommand());
    for (final CommandNode<CommandSourceStack> child : destination.getChildren()) {
      builder.then(child);
    }
    return builder.build();
  }

  @Override
  public boolean hasPermission(final Commander sender, final String permission) {
    return permission.isEmpty() || sender.hasPermission(permission);
  }

  @Override
  public CommandMeta createDefaultCommandMeta() {
    return SimpleCommandMeta.empty();
  }

  @Override
  public SenderMapper<CommandSourceStack, Commander> senderMapper() {
    return this.senderMapper;
  }

  @Override
  public boolean hasBrigadierManager() {
    return true;
  }

  @Override
  public CloudBrigadierManager<Commander, CommandSourceStack> brigadierManager() {
    return this.brigadierManager;
  }

  @Override
  public SuggestionFactory<Commander, ? extends TooltipSuggestion> suggestionFactory() {
    return this.suggestionFactory;
  }

  private static final class Registration implements CommandRegistrationHandler<Commander> {
    private final Set<Command<Commander>> commands = ConcurrentHashMap.newKeySet();

    @Override
    public boolean registerCommand(final Command<Commander> command) {
      return this.commands.add(command);
    }
  }
}
