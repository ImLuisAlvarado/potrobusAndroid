package mx.itson.potrobus.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import mx.itson.potrobus.R
import mx.itson.potrobus.entities.Parada

class ParadasAdapter(private val paradas: List<Parada>) :
    RecyclerView.Adapter<ParadasAdapter.ViewHolder>() {

    private var indiceActual = -1

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNombre: TextView = view.findViewById(R.id.tvParadaNombre)
        val tvOrden: TextView = view.findViewById(R.id.tvParadaOrden)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parada, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val parada = paradas[position]
        holder.tvNombre.text = parada.nombre
        holder.tvOrden.text = when (position) {
            0 -> "Salida"
            paradas.size - 1 -> "ITSON"
            else -> "Parada ${"%02d".format(position)}"
        }

        val puntoParada = holder.itemView.findViewById<View>(R.id.puntoParada)
        val lineaArriba = holder.itemView.findViewById<View>(R.id.lineaArriba)
        val lineaAbajo  = holder.itemView.findViewById<View>(R.id.lineaAbajo)

        when (position) {
            0 -> {
                lineaArriba.visibility = View.INVISIBLE
                lineaAbajo.visibility  = View.VISIBLE
            }
            paradas.size - 1 -> {
                lineaArriba.visibility = View.VISIBLE
                lineaAbajo.visibility  = View.INVISIBLE
            }
            else -> {
                lineaArriba.visibility = View.VISIBLE
                lineaAbajo.visibility  = View.VISIBLE
            }
        }

        val dp = holder.itemView.context.resources.displayMetrics.density
        when (position) {
            0, paradas.size - 1 -> {
                puntoParada.layoutParams.width  = (15 * dp).toInt()
                puntoParada.layoutParams.height = (15 * dp).toInt()
            }
            else -> {
                puntoParada.layoutParams.width  = (10 * dp).toInt()
                puntoParada.layoutParams.height = (10 * dp).toInt()
            }
        }

        val yapaso        = position <= indiceActual
        val colorLinea    = if (yapaso) "#1565C0" else "#BBBBBB"
        val colorTexto    = if (yapaso) "#333333" else "#BBBBBB"
        val drawableCirculo = if (yapaso) R.drawable.circle_blue else R.drawable.circle_gray

        puntoParada.setBackgroundResource(drawableCirculo)
        lineaArriba.setBackgroundColor(Color.parseColor(colorLinea))
        lineaAbajo.setBackgroundColor(Color.parseColor(colorLinea))
        holder.tvOrden.setTextColor(Color.parseColor(colorTexto))
        holder.tvNombre.setTextColor(Color.parseColor(colorTexto))

        puntoParada.requestLayout()
        puntoParada.invalidate()
    }

    override fun getItemCount() = paradas.size

    fun actualizarProgreso(indice: Int) {
        indiceActual = indice
        notifyDataSetChanged()
    }
}