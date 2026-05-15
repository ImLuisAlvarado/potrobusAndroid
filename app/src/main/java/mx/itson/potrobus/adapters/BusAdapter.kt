package mx.itson.potrobus.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.itson.potrobus.R
import mx.itson.potrobus.entities.Unidad

class BusAdapter(
    private val buses: List<Unidad>,
    private val onClick: (Unidad) -> Unit
) : RecyclerView.Adapter<BusAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumero: TextView = view.findViewById(R.id.tvNumeroEconomico)
        val tvPlaca: TextView  = view.findViewById(R.id.tvPlaca)
        val tvModelo: TextView = view.findViewById(R.id.tvModelo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val bus = buses[position]
        holder.tvNumero.text = "Unidad ${bus.numero_economico}"
        holder.tvPlaca.text  = bus.placa
        holder.tvModelo.text = bus.modelo ?: ""
        holder.itemView.setOnClickListener { onClick(bus) }
    }

    override fun getItemCount() = buses.size
}