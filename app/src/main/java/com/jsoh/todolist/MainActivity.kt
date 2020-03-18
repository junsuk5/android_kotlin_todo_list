package com.jsoh.todolist

import android.app.Activity
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.jsoh.todolist.databinding.ActivityMainBinding
import com.jsoh.todolist.databinding.ItemTodoBinding

class MainActivity : AppCompatActivity() {
    val RC_SIGN_IN = 1000

    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        // 로그인이 안 됨
        if (FirebaseAuth.getInstance().currentUser == null) {
            login()
        }


        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = TodoAdapter(
                emptyList(),
                onClickDeleteIcon = {
                    viewModel.deleteTodo(it)
                },
                onClickItem = {
                    viewModel.toggleTodo(it)
                }
            )
        }

        binding.addButton.setOnClickListener {
            val todo = Todo(binding.editText.text.toString())
            viewModel.addTodo(todo)
        }

        // 관찰 UI 업데이트
        viewModel.todoLiveData.observe(this, Observer {
            (binding.recyclerView.adapter as TodoAdapter).setData(it)
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                viewModel.fetchData()
            } else {
                // 로그인 실패
                finish()
            }
        }
    }

    fun login() {
        val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())

        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN)
    }

    fun logout() {
        AuthUI.getInstance()
            .signOut(this)
            .addOnCompleteListener {
                login()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.action_log_out -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}

data class Todo(
    val text: String,
    var isDone: Boolean = false
)

class TodoAdapter(
    private var myDataset: List<Todo>,
    val onClickDeleteIcon: (todo: Todo) -> Unit,
    val onClickItem: (todo: Todo) -> Unit
) :
    RecyclerView.Adapter<TodoAdapter.TodoViewHolder>() {

    class TodoViewHolder(val binding: ItemTodoBinding) : RecyclerView.ViewHolder(binding.root)


    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TodoAdapter.TodoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_todo, parent, false)
        return TodoViewHolder(ItemTodoBinding.bind(view))
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        val todo = myDataset[position]
        holder.binding.todoText.text = todo.text

        if (todo.isDone) {
            holder.binding.todoText.apply {
                paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                setTypeface(null, Typeface.ITALIC)
            }
        } else {
            holder.binding.todoText.apply {
                paintFlags = 0
                setTypeface(null, Typeface.NORMAL)
            }
        }

        holder.binding.deleteImageView.setOnClickListener {
            onClickDeleteIcon.invoke(todo)
        }

        holder.binding.root.setOnClickListener {
            onClickItem.invoke(todo)
        }
    }

    override fun getItemCount() = myDataset.size

    fun setData(newData: List<Todo>) {
        myDataset = newData
        notifyDataSetChanged()
    }
}

class MainViewModel: ViewModel() {
    val db = Firebase.firestore

    val todoLiveData = MutableLiveData<List<Todo>>()

    private val data = arrayListOf<Todo>()

    init {
        fetchData()
    }

    fun fetchData() {
        db.collection("todos")
            .get()
            .addOnSuccessListener { result ->
                data.clear()
                for (document in result) {
                    val todo = Todo(
                        document.data["text"] as String,
                        document.data["isDone"] as Boolean
                    )
                    data.add(todo)
                }
                todoLiveData.value = data
            }
    }

    fun toggleTodo(todo: Todo) {
        todo.isDone = !todo.isDone
        todoLiveData.value = data
    }

    fun addTodo(todo: Todo) {
        data.add(todo)
        todoLiveData.value = data
    }

    fun deleteTodo(todo: Todo) {
        data.remove(todo)
        todoLiveData.value = data
    }
}