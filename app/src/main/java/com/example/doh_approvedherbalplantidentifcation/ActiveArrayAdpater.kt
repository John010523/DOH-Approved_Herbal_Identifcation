import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView

class ActiveColorAdapter(context: Context, resource: Int, objects: Array<String>) :
    ArrayAdapter<String>(context, resource, objects) {

    private val activeGreen = android.graphics.Color.parseColor("#63FF14")

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val tv = view as TextView

        // If this item is the one currently selected in the spinner, make it green
        if (position == (parent as AdapterView<*>).selectedItemPosition) {
            tv.setTextColor(activeGreen)
            tv.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            tv.setTextColor(android.graphics.Color.BLACK) // Default color for others
            tv.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        return view
    }
}