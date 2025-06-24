package matiastgm.com.br.quest

import com.cobblemon.mod.common.pokemon.Pokemon
import net.minecraft.nbt.CompoundTag

data class PokemonEntry(
    var type: String = "",
    var isShiny: Boolean = false,
    var minLevel: Int = 0
) : Comparable<PokemonEntry> {

    constructor(type: String) : this(type, false)
    constructor(type: String, isShiny: Boolean) : this(type, isShiny, 0)

    override fun toString(): String {
        return "PokemonEntry(type='$type', isShiny=$isShiny, minLevel=$minLevel)"
    }

    override fun compareTo(other: PokemonEntry): Int {
        return compareValuesBy(this, other,
            { it.type },
            { it.isShiny },
            { it.minLevel }
        )
    }

    fun toNbt(): CompoundTag {
        val tag = CompoundTag()
        tag.putString("type", type)
        tag.putBoolean("isShiny", isShiny)
        tag.putInt("minLevel", minLevel)
        return tag
    }

    fun fromNbt(nbt: CompoundTag) {
        type = nbt.getString("type")
        isShiny = nbt.getBoolean("isShiny")
        minLevel = nbt.getInt("minLevel")
    }

    fun matches(pokemon: Pokemon): Boolean {
        if (pokemon.species.resourceIdentifier.toString() != type) return false
        if (pokemon.shiny != isShiny) return false
        if (pokemon.level < minLevel) return false
        return true
    }
}
