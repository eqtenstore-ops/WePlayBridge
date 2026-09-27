
package com.you.weplaybridge
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
data class Song(val id: Long, val title: String, val path: String)
class MainActivity : AppCompatActivity() {
    private val songs = mutableListOf<Song>()
    private lateinit var adapter: SongAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SongAdapter(songs) { send(it) }
        recycler.adapter = adapter
        findViewById<Button>(R.id.btnLoad).setOnClickListener { checkPerm() }
        checkPerm()
    }
    private fun checkPerm() {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, arrayOf(perm), 100)
        else load()
    }
    private fun load() {
        songs.clear()
        val proj = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA)
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj, null, null, null)?.use { c ->
            val idC = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val tC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dC = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) {
                val p = c.getString(dC) ?: ""
                if (File(p).exists()) songs.add(Song(c.getLong(idC), c.getString(tC) ?: "", p))
            }
        }
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "${songs.size} musiques", Toast.LENGTH_SHORT).show()
    }
    private fun send(s: Song) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", File(s.path))
            val intent = Intent(Intent.ACTION_SEND).apply { type = "audio/*"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); setPackage("com.wejoy.weplay") }
            startActivity(Intent.createChooser(intent, "WePlay"))
        } catch (e: Exception) { Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show() }
    }
}
class SongAdapter(private val list: List<Song>, private val onSend: (Song) -> Unit) : RecyclerView.Adapter<SongAdapter.VH>() {
    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) { val tv = v.findViewById<android.widget.TextView>(R.id.tvTitle); val btn = v.findViewById<android.widget.Button>(R.id.btnSend) }
    override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_song, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, i: Int) { val s = list[i]; h.tv.text = s.title; h.btn.setOnClickListener { onSend(s) } }
}
