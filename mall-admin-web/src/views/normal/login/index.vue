<script lang="ts" setup>
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user';
import { isvalidUsername } from '@/utils/validate';
import login_center_bg from '@/assets/images/login_center_bg.png'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import { adminRegisterAPI } from '@/apis/admin'

// 使用Vue Router和Pinia
const router = useRouter()
const userStore = useUserStore()

// 表单引用
const loginFormRef = ref<FormInstance>()

// 登陆表单数据
const loginForm = reactive({
  username: '',
  password: '',
})

// 表单参数校验规则
const loginRules = reactive<FormRules<typeof loginForm>>({
  username: [{ required: true, trigger: 'blur', validator: validateUsername }],
  password: [{ required: true, trigger: 'blur', validator: validatePass }]
})

// 登陆按钮进度条
const loading = ref(false)

// 注册对话框
const registerDialogVisible = ref(false)
const registering = ref(false)
const registerFormRef = ref<FormInstance>()
const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})
const registerRules = reactive<FormRules<typeof registerForm>>({
  username: [{ required: true, trigger: 'blur', validator: validateUsername }],
  password: [{ required: true, trigger: 'blur', validator: validateRegisterPass }],
  confirmPassword: [{ required: true, trigger: 'blur', validator: validateConfirmPass }],
})

// 用户名验证函数
function validateUsername(rule: unknown, value: string, callback: (error?: Error) => void) {
  if (!isvalidUsername(value)) {
    callback(new Error('请输入正确的用户名'))
  } else {
    callback()
  }
};

// 密码验证函数
function validatePass(rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value.length < 3) {
    callback(new Error('密码不能小于3位'))
  } else {
    callback()
  }
};

// 注册密码验证函数
function validateRegisterPass(rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value.length < 6) {
    callback(new Error('密码不能小于6位'))
  } else {
    callback()
  }
};

// 确认密码验证函数
function validateConfirmPass(rule: unknown, value: string, callback: (error?: Error) => void) {
  if (value !== registerForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
};

// 处理注册按钮事件
const handleRegister = () => {
  registerFormRef.value!.validate(async (valid) => {
    if (valid) {
      registering.value = true
      try {
        await adminRegisterAPI({
          username: registerForm.username.trim(),
          password: registerForm.password,
        })
        ElMessage.success('注册成功，请登录')
        loginForm.username = registerForm.username.trim()
        registerDialogVisible.value = false
        registerForm.username = ''
        registerForm.password = ''
        registerForm.confirmPassword = ''
      } catch (err) {
        console.log(err)
      } finally {
        registering.value = false
      }
    }
  })
}

// 组件挂载完成后调用
onMounted(() => {
  loginForm.username = userStore.userInfo.username
  loginForm.password = userStore.userInfo.password
  if (loginForm.username === undefined || loginForm.username == null || loginForm.username === '') {
    loginForm.username = 'admin';
  }
})

// 处理登录按钮事件
const handleLogin = () => {
  loginFormRef.value!.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await userStore.userLogin({
          username: loginForm.username.trim(),
          password: loginForm.password
        })
        loading.value = false
        router.push({ path: '/' })
      }
      catch (err) {
        loading.value = false
        console.log(err)
      }
    } else {
      console.log('参数验证不合法！')
    }
  })
}
</script>

<template>
  <div>
    <el-card class="login-form-layout">
      <el-form autoComplete="on" :model="loginForm" :rules="loginRules" ref="loginFormRef" label-position="left">
        <div style="text-align: center">
          <svg-icon icon-class="login-mall" style="width: 56px;height: 56px;color: #409EFF"></svg-icon>
        </div>
        <h2 class="login-title color-main">mall-admin-web</h2>
        <el-form-item prop="username">
          <el-input name="username" type="text" v-model="loginForm.username" autoComplete="on" placeholder="请输入用户名">
            <template #prefix>
              <span>
                <svg-icon icon-class="user" class="color-main"></svg-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input name="password" @keyup.enter="handleLogin" v-model="loginForm.password" autoComplete="on"
            show-password placeholder="请输入密码">
            <template #prefix>
              <span>
                <svg-icon icon-class="password" class="color-main"></svg-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item style="margin-bottom: 60px;text-align: center">
          <el-button style="width: 45%" type="primary" :loading="loading" @click="handleLogin">
            登录
          </el-button>
          <el-button style="width: 45%" type="primary" @click="registerDialogVisible = true">
            注册
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <img :src="login_center_bg" class="login-center-layout">
    <el-dialog title="注册账号" v-model="registerDialogVisible" :center="true" width="30%">
      <el-form :model="registerForm" :rules="registerRules" ref="registerFormRef" label-position="left">
        <el-form-item prop="username">
          <el-input v-model="registerForm.username" placeholder="请输入用户名">
            <template #prefix>
              <span>
                <svg-icon icon-class="user" class="color-main"></svg-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="registerForm.password" show-password placeholder="请输入密码">
            <template #prefix>
              <span>
                <svg-icon icon-class="password" class="color-main"></svg-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" show-password placeholder="请再次输入密码">
            <template #prefix>
              <span>
                <svg-icon icon-class="password" class="color-main"></svg-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="registerDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="registering" @click="handleRegister">注册</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.login-form-layout {
  position: absolute;
  left: 0;
  right: 0;
  width: 360px;
  margin: 140px auto;
  border-top: 10px solid #409EFF;
}

.login-title {
  text-align: center;
}

.login-center-layout {
  background: #409EFF;
  width: auto;
  height: auto;
  max-width: 100%;
  max-height: 100%;
  margin-top: 200px;
}
</style>
