
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
    private val REQ_PERM = 1001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val recycler = findViewById<RecyclerView>(R.id.recycler)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SongAdapter(songs) { song -> sendToWePlay(song) }
        recycler.adapter = adapter
        findViewById<Button>(R.id.btnLoad).setOnClickListener { checkPerm() }
        checkPerm()
    }
    private fun checkPerm() {
        val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(perm), REQ_PERM)
        } else loadSongs()
    }
    private fun loadSongs() {
        songs.clear()
        val projection = arrayOf(MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DATA)
        contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val title = c.getString(titleCol) ?: "Unknown"
                val path = c.getString(dataCol) ?: ""
                if (File(path).exists()) songs.add(Song(id, title, path))
            }
        }
        adapter.notifyDataSetChanged()
        Toast.makeText(this, "${songs.size} musiques", Toast.LENGTH_SHORT).show()
    }
    private fun sendToWePlay(song: Song) {
        try {
            val file = File(song.path)
            val uri = FileProvider.getUriForFile(this, "$packageName.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.wejoy.weplay")
            }
            startActivity(Intent.createChooser(intent, "Envoyer vers WePlay"))
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    override fun onRequestPermissionsResult(code: Int, perms: Array<String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (code == REQ_PERM && res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) loadSongs()
    }
}
class SongAdapter(private val list: List<Song>, private val onSend: (Song) -> Unit) : RecyclerView.Adapter<SongAdapter.VH>() {
    class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
        val tv = v.findViewById<android.widget.TextView>(R.id.tvTitle)
        val btn = v.findViewById<android.widget.Button>(R.id.btnSend)
    }
    override fun onCreateViewHolder(p: android.view.ViewGroup, t: Int) = VH(android.view.LayoutInflater.from(p.context).inflate(R.layout.item_song, p, false))
    override fun getItemCount() = list.size
    override fun onBindViewHolder(h: VH, i: Int) {
        val s = list[i]
        h.tv.text = s.title
        h.btn.setOnClickListener { onSend(s) }
    }
}
