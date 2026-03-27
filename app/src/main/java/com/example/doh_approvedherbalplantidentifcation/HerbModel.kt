package com.example.doh_approvedherbalplantidentifcation

data class HerbModel(
    val id: Int,
    val herbalname: String,       // matches COL_NAME
    val herballevel: Float,       // matches COL_LEVEL
    val herbaldescription: String,// matches COL_DESCRIPTION
    val herbalsafetywarn: String, // matches COL_SAFETY
    val herbalimage: ByteArray    // matches COL_IMAGE
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HerbModel

        if (id != other.id) return false
        if (herballevel != other.herballevel) return false
        if (herbalname != other.herbalname) return false
        if (herbaldescription != other.herbaldescription) return false
        if (herbalsafetywarn != other.herbalsafetywarn) return false
        if (!herbalimage.contentEquals(other.herbalimage)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + herballevel.hashCode()
        result = 31 * result + herbalname.hashCode()
        result = 31 * result + herbaldescription.hashCode()
        result = 31 * result + herbalsafetywarn.hashCode()
        result = 31 * result + herbalimage.contentHashCode()
        return result
    }
}