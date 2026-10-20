<template>
  <view class="container">
    <view class="left-bottom-sign"></view>
    <view class="back-btn yticon icon-zuojiantou-up" @click="navBack"></view>
    <view class="right-top-sign"></view>

    <!-- 原页面：二维码关注 -->
    <view class="wrapper" v-if="pageMode === 'qrcode'">
      <view class="empty">
        <image src="/static/qrcode_for_macrozheng_258.jpg" mode="aspectFit"></image>
        <view class="empty-tips"> 扫描上方二维码<view class="navigator">关注公众号</view>, </view>
        <view class="empty-tips"> 回复<view class="navigator">会员</view>获取体验账号。 </view>
      </view>
    </view>

    <!-- 注册表单 -->
    <view class="wrapper" v-if="pageMode === 'register'">
      <view class="left-top-sign">REGISTER</view>
      <view class="welcome">注册账号！</view>
      <view class="input-content">
        <view class="input-item">
          <text class="tit">用户名</text>
          <input
            type="text"
            v-model="formData.username"
            placeholder="请输入用户名"
            :maxlength="20"
          />
        </view>
        <view class="input-item">
          <text class="tit">密码</text>
          <input
            v-model="formData.password"
            placeholder="8-18位不含特殊字符的数字、字母组合"
            placeholder-class="input-empty"
            :maxlength="20"
            password
          />
        </view>
        <view class="input-item">
          <text class="tit">确认密码</text>
          <input
            v-model="formData.confirmPassword"
            placeholder="请再次输入密码"
            placeholder-class="input-empty"
            :maxlength="20"
            password
          />
        </view>
      </view>
      <button class="confirm-btn" @click="handleRegister" :disabled="registering"> 注册 </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { registerAPI } from '@/apis/member'

// ===== 页面数据 =====
// 页面模式：qrcode=二维码页面, register=注册表单
const pageMode = ref<'qrcode' | 'register'>('qrcode')
// 注册表单数据
const formData = ref({
  username: '',
  password: '',
  confirmPassword: '',
})
// 注册加载状态
const registering = ref(false)

// ===== 生命周期 =====
// 页面加载时根据参数设置模式
onLoad((options) => {
  if (options?.mode === 'register') {
    pageMode.value = 'register'
  } else {
    pageMode.value = 'qrcode'
  }
})

// 注册
const handleRegister = async () => {
  const { username, password, confirmPassword } = formData.value
  if (!username) {
    uni.showToast({ title: '请输入用户名', icon: 'none' })
    return
  }
  if (!password) {
    uni.showToast({ title: '请输入密码', icon: 'none' })
    return
  }
  if (password.length < 6) {
    uni.showToast({ title: '密码不能小于6位', icon: 'none' })
    return
  }
  if (password !== confirmPassword) {
    uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
    return
  }

  registering.value = true
  try {
    await registerAPI({ username, password })
    uni.showToast({
      title: '注册成功',
      icon: 'success',
    })
    setTimeout(() => {
      uni.redirectTo({ url: '/pages/public/login' })
    }, 1500)
  } catch {
    uni.showToast({
      title: '注册失败，请重试',
      icon: 'none',
    })
  } finally {
    registering.value = false
  }
}

// 返回上一页
const navBack = () => {
  uni.navigateBack()
}
</script>

<style lang="scss">
page {
  background: #fff;
}
</style>

<style lang="scss" scoped>
.container {
  padding-top: 115px;
  position: relative;
  width: 100vw;
  height: 100vh;
  overflow: hidden;
  background: #fff;
}

.wrapper {
  position: relative;
  z-index: 90;
  background: #fff;
  padding-bottom: 40rpx;
}

.back-btn {
  position: absolute;
  left: 40rpx;
  z-index: 9999;
  top: calc(var(--status-bar-height) + 40rpx);
  font-size: 40rpx;
  color: $font-color-dark;
}

.left-top-sign {
  font-size: 120rpx;
  color: $page-color-base;
  position: relative;
  left: -16rpx;
}

.right-top-sign {
  position: absolute;
  top: calc(var(--status-bar-height) + 80rpx);
  right: -30rpx;
  z-index: 95;

  &:before,
  &:after {
    display: block;
    content: '';
    width: 400rpx;
    height: 80rpx;
    background: #b4f3e2;
  }

  &:before {
    transform: rotate(50deg);
    border-radius: 0 50px 0 0;
  }

  &:after {
    position: absolute;
    right: -198rpx;
    top: 0;
    transform: rotate(-50deg);
    border-radius: 50px 0 0 0;
  }
}

.left-bottom-sign {
  position: absolute;
  left: -270rpx;
  bottom: -320rpx;
  border: 100rpx solid #d0d1fd;
  border-radius: 50%;
  padding: 180rpx;
}

.welcome {
  position: relative;
  left: 50rpx;
  top: -90rpx;
  font-size: 46rpx;
  color: #555;
  text-shadow: 1px 0px 1px rgba(0, 0, 0, 0.3);
}

.empty {
  position: fixed;
  left: 0;
  top: 0;
  width: 100%;
  height: 100vh;
  padding-bottom: 100rpx;
  display: flex;
  justify-content: center;
  flex-direction: column;
  align-items: center;
  background: #fff;

  image {
    width: 420rpx;
    height: 420rpx;
    margin-bottom: 30rpx;
  }

  .empty-tips {
    display: flex;
    font-size: 28rpx;
    color: $font-color-disabled;

    .navigator {
      color: $uni-color-primary;
      margin-left: 0rpx;
    }
  }
}

.input-content {
  padding: 0 60rpx;
}

.input-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 0 30rpx;
  background: $page-color-light;
  height: 120rpx;
  border-radius: 4px;
  margin-bottom: 50rpx;

  &:last-child {
    margin-bottom: 0;
  }

  .tit {
    height: 50rpx;
    line-height: 56rpx;
    font-size: $font-sm + 2rpx;
    color: $font-color-base;
  }

  input {
    height: 60rpx;
    font-size: $font-base + 2rpx;
    color: $font-color-dark;
    width: 100%;
  }
}

.confirm-btn {
  width: 630rpx;
  height: 76rpx;
  line-height: 76rpx;
  border-radius: 50px;
  margin-top: 70rpx;
  background: $uni-color-primary;
  color: #fff;
  font-size: $font-lg;

  &:after {
    border-radius: 100px;
  }
}
</style>
