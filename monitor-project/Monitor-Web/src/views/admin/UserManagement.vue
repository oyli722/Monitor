<template>
  <div class="user-management">
    <div class="action-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索用户名或邮箱..."
        :prefix-icon="Search"
        clearable
        style="width: 300px"
      />
      <el-button type="primary" :icon="Plus" @click="handleAddUser">
        添加用户
      </el-button>
    </div>

    <el-table :data="filteredUsers" stripe style="width: 100%">
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.role === 'admin' ? 'danger' : 'primary'">
            {{ row.role === 'admin' ? '管理员' : '普通用户' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="handleEditUser(row)">
            编辑
          </el-button>
          <el-button link type="danger" @click="handleDeleteUser(row)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '添加用户'"
      width="500px"
    >
      <el-form :model="userForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="userForm.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="userForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="userForm.role" style="width: 100%">
            <el-option label="普通用户" value="user" />
            <el-option label="管理员" value="admin" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码">
          <el-input
            v-model="userForm.password"
            type="password"
            placeholder="请输入密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { Search, Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'

interface User {
  id: string
  username: string
  email: string
  role: 'admin' | 'user'
  createdAt: string
}

const searchKeyword = ref('')
const dialogVisible = ref(false)
const isEdit = ref(false)

const userForm = reactive({
  id: '',
  username: '',
  email: '',
  role: 'user' as 'admin' | 'user',
  password: '',
})

const users = ref<User[]>([
  {
    id: '1',
    username: 'admin',
    email: 'admin@example.com',
    role: 'admin',
    createdAt: '2024-01-01',
  },
  {
    id: '2',
    username: 'user',
    email: 'user@example.com',
    role: 'user',
    createdAt: '2024-01-15',
  },
  {
    id: '3',
    username: 'developer',
    email: 'dev@example.com',
    role: 'user',
    createdAt: '2024-01-20',
  },
])

const filteredUsers = computed(() => {
  if (!searchKeyword.value) return users.value
  const keyword = searchKeyword.value.toLowerCase()
  return users.value.filter(
    (u) =>
      u.username.toLowerCase().includes(keyword) ||
      u.email.toLowerCase().includes(keyword),
  )
})

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleString('zh-CN')
}

function handleAddUser() {
  isEdit.value = false
  userForm.id = ''
  userForm.username = ''
  userForm.email = ''
  userForm.role = 'user'
  userForm.password = ''
  dialogVisible.value = true
}

function handleEditUser(user: User) {
  isEdit.value = true
  userForm.id = user.id
  userForm.username = user.username
  userForm.email = user.email
  userForm.role = user.role
  userForm.password = ''
  dialogVisible.value = true
}

function handleSave() {
  if (isEdit.value) {
    const index = users.value.findIndex((u) => u.id === userForm.id)
    if (index !== -1) {
      users.value[index] = {
        ...users.value[index],
        username: userForm.username,
        email: userForm.email,
        role: userForm.role,
      }
      ElMessage.success('更新成功')
    }
  } else {
    users.value.push({
      id: Date.now().toString(),
      username: userForm.username,
      email: userForm.email,
      role: userForm.role,
      createdAt: new Date().toISOString(),
    })
    ElMessage.success('添加成功')
  }
  dialogVisible.value = false
}

function handleDeleteUser(user: User) {
  ElMessageBox.confirm(`确定要删除用户 ${user.username} 吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(() => {
    const index = users.value.findIndex((u) => u.id === user.id)
    if (index !== -1) {
      users.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}
</script>

<style scoped>
.user-management {
  padding: 20px;
}

.action-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
</style>
