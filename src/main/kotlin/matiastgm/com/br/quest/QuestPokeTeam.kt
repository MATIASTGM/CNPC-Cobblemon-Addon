package matiastgm.com.br.quest

import com.cobblemon.mod.common.Cobblemon
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import com.cobblemon.mod.common.api.storage.NoPokemonStoreException
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore
import com.cobblemon.mod.common.pokemon.Species
import matiastgm.com.br.util.NBTUtils
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import noppes.npcs.api.CustomNPCsException
import noppes.npcs.api.handler.data.IQuestObjective
import noppes.npcs.quests.QuestInterface

class QuestPokeTeam : QuestInterface() {
    var targets: MutableList<PokemonEntry> = mutableListOf()

    // Se seu ambiente pedir, use essas assinaturas (descomente e comente as antigas):
    override fun readAdditionalSaveData(provider: HolderLookup.Provider, compound: CompoundTag) {
        targets = NBTUtils.getNBTList(
            factory = { PokemonEntry() },
            fromNbt = { obj, tag -> obj.fromNbt(tag) },
            tagList = compound.getList("QuestTeamTargets", 10)
        )
    }

    override fun addAdditionalSaveData(provider: HolderLookup.Provider, compound: CompoundTag) {
        compound.put("QuestTeamTargets", NBTUtils.tagListToNBT(targets) { it.toNbt() })
    }

    // Se não pedir o provider, mantenha só assim:
    // override fun readAdditionalSaveData(compound: CompoundTag) { ... }
    // override fun addAdditionalSaveData(compound: CompoundTag) { ... }



    override fun isCompleted(player: Player): Boolean {
        return try {
            val store = Cobblemon.storage.getParty(player.uuid) // Não faça cast!
            for (entry in targets) {
                var hasInTeam = false
                for (pokemon in store) {
                    if (entry.matches(pokemon)) {
                        hasInTeam = true
                    }
                }
                if (!hasInTeam) return false
            }
            true
        } catch (ignored: NoPokemonStoreException) {
            false
        }
    }


    override fun handleComplete(player: Player) {
        // Pode adicionar lógica extra de recompensa ou feedback aqui
    }

    override fun getObjectives(player: Player): Array<IQuestObjective> {
        return targets.map { QuestPokeTeamObjective(player, it) }.toTypedArray()
    }

    inner class QuestPokeTeamObjective(
         val player: Player,
         val pokemonEntry: PokemonEntry
    ) : IQuestObjective {

        override fun getProgress(): Int {
            return try {
                // Usa UUID como no Java, sem cast!
                val store: PlayerPartyStore = Cobblemon.storage.getParty(player.uuid)
                for (pokemon in store) {
                    if (pokemonEntry.matches(pokemon)) return 1
                }
                0
            } catch (ignored: NoPokemonStoreException) {
                0
            }
        }

        override fun setProgress(progress: Int) {
            throw CustomNPCsException("Cant set the progress of PokeTeamQuests")
        }

        override fun getMaxProgress(): Int = 1

        override fun isCompleted(): Boolean = getProgress() == 1

        override fun getText(): String = getMCText().string

        override fun getMCText(): Component {
            val text: MutableComponent = Component.translatable("objective.poketeam").append(" ")
            if (pokemonEntry.isShiny) {
                text.append(Component.translatable("poketype.shiny").append(" "))
            }
            if (pokemonEntry.minLevel != 0) {
                text.append(Component.translatable("poketype.minlevel", pokemonEntry.minLevel).append(" "))
            }
            val identifier = if (':' in pokemonEntry.type) pokemonEntry.type else "cobblemon:${pokemonEntry.type}"
            val species: Species? = PokemonSpecies.getByIdentifier(ResourceLocation.parse(identifier))
            text.append(species?.translatedName ?: Component.translatable(identifier))
            return text.append(": ${getProgress()}/${getMaxProgress()}")
        }
    }
}
