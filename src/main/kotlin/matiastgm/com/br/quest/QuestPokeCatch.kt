package matiastgm.com.br.quest

import com.cobblemon.mod.common.api.pokemon.PokemonSpecies
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.player.Player
import noppes.npcs.controllers.data.PlayerData
import noppes.npcs.controllers.data.QuestData
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import net.minecraft.resources.ResourceLocation
import com.cobblemon.mod.common.pokemon.Species
import matiastgm.com.br.constants.PokeQuestType
import matiastgm.com.br.util.NBTUtils
import net.minecraft.core.HolderLookup
import noppes.npcs.api.CustomNPCsException
import noppes.npcs.api.handler.data.IQuestObjective
import noppes.npcs.quests.QuestInterface
import java.util.TreeMap
import kotlin.collections.iterator

class QuestPokeCatch : QuestInterface() {
  public var targets: MutableMap<PokemonEntry, Int> = sortedMapOf()

    override fun readAdditionalSaveData(provider: HolderLookup.Provider, compound: CompoundTag) {
        targets = TreeMap(
            NBTUtils.getNBTIntegerMap(
                factory = { PokemonEntry() },
                fromNbt = { obj, tag -> obj.fromNbt(tag) },
                tagList = compound.getList("QuestCatchTargets", 10)
            )
        )
    }

    override fun addAdditionalSaveData(provider: HolderLookup.Provider, compound: CompoundTag) {
        compound.put("QuestCatchTargets", NBTUtils.tagIntegerMapToNBT(targets) { it.toNbt() })
    }

    override fun isCompleted(player: Player): Boolean {
        val playerdata = PlayerData.get(player).questData
        val data = playerdata.activeQuests[questId] ?: return false
        val killed = getCaught(data)
        if (killed.size != targets.size) return false
        for (entity in killed.keys) {
            if (!targets.containsKey(entity) || targets[entity]!! > killed[entity]!!) return false
        }
        return true
    }

    override fun handleComplete(player: Player) {
        // Completar alguma lógica extra, se necessário
    }

    fun getCaught(data: QuestData): HashMap<PokemonEntry, Int> {
        return NBTUtils.getNBTIntegerMap(
            factory = { PokemonEntry() },
            fromNbt = { obj, tag -> obj.fromNbt(tag) },
            tagList = data.extraData.getList("Caught", 10)
        )
    }

    fun setCaught(data: QuestData, killed: HashMap<PokemonEntry, Int>) {
        data.extraData.put("Caught", NBTUtils.tagIntegerMapToNBT(killed) { it.toNbt() })
    }

    override fun getObjectives(player: Player): Array<IQuestObjective> {
        val list = mutableListOf<IQuestObjective>()
        for ((entry, amount) in targets) {
            list.add(QuestPokeCatchObjective(player, entry, amount))
        }
        return list.toTypedArray()
    }

    inner class QuestPokeCatchObjective(
        private val player: Player,
        private val pokemonEntry: PokemonEntry,
        private val amount: Int
    ) : IQuestObjective {
        override fun getProgress(): Int {
            val data = PlayerData.get(player)
            val playerdata = data.questData
            val questdata = playerdata.activeQuests[questId] ?: return 0
            val caught = getCaught(questdata)
            return caught[pokemonEntry] ?: 0
        }

        override fun setProgress(progress: Int) {
            if (progress < 0 || progress > amount) {
                throw CustomNPCsException("Progress has to be between 0 and $amount")
            }
            val data = PlayerData.get(player)
            val playerdata = data.questData
            val questdata = playerdata.activeQuests[questId] ?: return
            val caught = getCaught(questdata)

            if (caught[pokemonEntry] == progress) {
                return
            }
            caught[pokemonEntry] = progress
            setCaught(questdata, caught)
            data.questData.checkQuestCompletion(player, PokeQuestType.POKE_CATCH)
            data.updateClient = true
        }

        override fun getMaxProgress(): Int = amount

        override fun isCompleted(): Boolean = getProgress() >= amount

        override fun getText(): String = getMCText().string

        override fun getMCText(): Component {
            val text: MutableComponent = Component.translatable("objective.pokecatch").append(" ")
            if (pokemonEntry.isShiny) {
                text.append(Component.translatable("poketype.shiny").append(" "))
            }
            val identifier = if (':' in pokemonEntry.type) pokemonEntry.type else "cobblemon:${pokemonEntry.type}"
            val species: Species? = PokemonSpecies.getByIdentifier(ResourceLocation.parse(identifier))
            text.append(species?.translatedName ?: Component.translatable(identifier))
            return text.append(": ${getProgress()}/${getMaxProgress()}")
        }

    }
}
