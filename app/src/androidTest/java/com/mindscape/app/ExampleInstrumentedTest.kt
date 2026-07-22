package com.mindscape.app

import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.reflect.Method
import java.lang.reflect.Field

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @Test
    fun testCategoryExistsLogic() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val categoriesListField: Field = MainActivity::class.java.getDeclaredField("categoriesList")
            categoriesListField.isAccessible = true
            val categoriesList = categoriesListField.get(activity) as MutableList<Any>

            categoriesList.clear()

            // Создаем корневую категорию "1"
            val category1 = Category("1", "Description", 0xFFFFFF, null)
            categoriesList.add(category1)

            val categoryExistsMethod: Method = MainActivity::class.java.getDeclaredMethod(
                "categoryExists",
                String::class.java,
                String::class.java
            )
            categoryExistsMethod.isAccessible = true

            // Проверяем, существует ли корневая категория "1"
            val exists1Root = categoryExistsMethod.invoke(activity, "1", null) as Boolean
            assertTrue("Корневая категория '1' должна существовать", exists1Root)

            // Проверяем, существует ли корневая категория "2"
            val exists2Root = categoryExistsMethod.invoke(activity, "2", null) as Boolean
            assertFalse("Корневая категория '2' не должна существовать", exists2Root)

            // Проверяем, существует ли подкатегория "1" под родителем "1"
            val exists1Under1 = categoryExistsMethod.invoke(activity, "1", "1") as Boolean
            assertFalse("Подкатегория '1' под родителем '1' еще не должна существовать", exists1Under1)

            // Добавляем подкатегорию "1" под родителем "1"
            val subCategory1 = Category("1", "Sub Description", 0xFFFFFF, "1")
            categoriesList.add(subCategory1)

            // Теперь проверяем, существует ли подкатегория "1" под родителем "1"
            val exists1Under1Now = categoryExistsMethod.invoke(activity, "1", "1") as Boolean
            assertTrue("Подкатегория '1' под родителем '1' теперь должна существовать", exists1Under1Now)
        }
        scenario.close()
    }

    @Test
    fun testConnectionsUniqueLogic() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val categoriesListField: Field = MainActivity::class.java.getDeclaredField("categoriesList")
            categoriesListField.isAccessible = true
            val categoriesList = categoriesListField.get(activity) as MutableList<Any>

            val connectionsListField: Field = MainActivity::class.java.getDeclaredField("connectionsList")
            connectionsListField.isAccessible = true
            val connectionsList = connectionsListField.get(activity) as MutableList<Any>

            categoriesList.clear()
            connectionsList.clear()

            // Создаем папки первого уровня "4" и "5"
            val folder4 = Category("4", "", 0, null)
            val folder5 = Category("5", "", 0, null)
            categoriesList.add(folder4)
            categoriesList.add(folder5)

            // Создаем родительскую папку "1" первого уровня
            val folder1 = Category("1", "", 0, null)
            categoriesList.add(folder1)

            // Создаем подпапки "4" и "5" внутри "1" (т.е. пути "1/4" and "1/5")
            val subFolder4 = Category("4", "", 0, "1")
            val subFolder5 = Category("5", "", 0, "1")
            categoriesList.add(subFolder4)
            categoriesList.add(subFolder5)

            // Имитируем связывание подпапок "1/4" и "1/5"
            // В Connection используется канонический prefixed ID.
            val connection = Connection("folder:1/4", "folder:1/5")
            connectionsList.add(connection)

            // Теперь проверяем, как Connections отображаются в JSON для WebView
            val buildGraphJsonMethod = MainActivity::class.java.getDeclaredMethod("buildGraphJson")
            buildGraphJsonMethod.isAccessible = true
            val jsonString = buildGraphJsonMethod.invoke(activity) as String
            
            val jsonObject = org.json.JSONObject(jsonString)
            val edges = jsonObject.getJSONArray("edges")

            // Должна быть связь между "1/4" и "1/5", но НЕ должно быть связи между "4" и "5"
            var foundTargetConnection = false
            var foundWrongConnection = false

            for (i in 0 until edges.length()) {
                val edge = edges.getJSONObject(i)
                val src = edge.getString("source")
                val tgt = edge.getString("target")

                if ((src == "folder:1/4" && tgt == "folder:1/5") || (src == "folder:1/5" && tgt == "folder:1/4")) {
                    foundTargetConnection = true
                }
                if ((src == "folder:4" && tgt == "folder:5") || (src == "folder:5" && tgt == "folder:4")) {
                    foundWrongConnection = true
                }
            }

            assertTrue("Связь между подпапками '1/4' и '1/5' должна быть в графе", foundTargetConnection)
            assertFalse("Связь между корневыми папками '4' и '5' НЕ должна создаваться", foundWrongConnection)
        }
        scenario.close()
    }

    @Test
    fun testDuplicateCenterLogic() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { activity ->
            val categoriesListField: Field = MainActivity::class.java.getDeclaredField("categoriesList")
            categoriesListField.isAccessible = true
            val categoriesList = categoriesListField.get(activity) as MutableList<Any>

            val notesListField: Field = MainActivity::class.java.getDeclaredField("notesList")
            notesListField.isAccessible = true
            val notesList = notesListField.get(activity) as MutableList<Any>

            categoriesList.clear()
            notesList.clear()

            // Создаем центр "A"
            val centerA = Category("A", "Center A Desc", 0xFFFFFF, null, true)
            categoriesList.add(centerA)

            // Создаем подпапку "Sub" внутри "A"
            val subFolder = Category("Sub", "Sub Desc", 0xFFFFFF, "A", false)
            categoriesList.add(subFolder)

            // Создаем заметку "Note" внутри "A"
            val note = Note("Note", "A", "Content")
            notesList.add(note)

            // Вызываем duplicateCenter
            val duplicateCenterMethod = MainActivity::class.java.getDeclaredMethod(
                "duplicateCenter",
                Category::class.java
            )
            duplicateCenterMethod.isAccessible = true
            duplicateCenterMethod.invoke(activity, centerA)

            // Проверяем, что создано
            // Должно быть:
            // 1. Центр "A (1)"
            // 2. Папка "Sub" под "A (1)" (т.е. fullPath = "A (1)/Sub")
            // 3. Заметка "Note (1)" под "A (1)" (т.е. categoryPath = "A (1)")
            
            val fullPathMethod = Category::class.java.getDeclaredMethod("fullPath")
            fullPathMethod.isAccessible = true

            val titleField = Category::class.java.getDeclaredField("title")
            titleField.isAccessible = true

            val parentField = Category::class.java.getDeclaredField("parent")
            parentField.isAccessible = true

            val isCenterField = Category::class.java.getDeclaredField("isCenter")
            isCenterField.isAccessible = true

            var foundDuplicatedCenter = false
            var foundDuplicatedSubFolder = false

            for (c in categoriesList) {
                val title = titleField.get(c) as String
                val parent = parentField.get(c) as String?
                val isCenter = isCenterField.get(c) as Boolean
                val fullPath = fullPathMethod.invoke(c) as String

                if (title == "A (1)" && parent == null && isCenter) {
                    foundDuplicatedCenter = true
                }
                if (title == "Sub (1)" && parent == "A (1)" && fullPath == "A (1)/Sub (1)") {
                    foundDuplicatedSubFolder = true
                }
            }

            assertTrue("Должен быть создан продублированный центр 'A (1)'", foundDuplicatedCenter)
            assertTrue("Должна быть создана продублированная подпапка 'A (1)/Sub (1)'", foundDuplicatedSubFolder)

            val noteTitleField = Note::class.java.getDeclaredField("title")
            noteTitleField.isAccessible = true
            val noteCatPathField = Note::class.java.getDeclaredField("categoryPath")
            noteCatPathField.isAccessible = true

            var foundDuplicatedNote = false
            for (n in notesList) {
                val title = noteTitleField.get(n) as String
                val catPath = noteCatPathField.get(n) as String?
                if (title == "Note (1)" && catPath == "A (1)") {
                    foundDuplicatedNote = true
                }
            }
            assertTrue("Должна быть создана продублированная заметка 'Note (1)' в категории 'A (1)'", foundDuplicatedNote)
        }
        scenario.close()
    }
}
