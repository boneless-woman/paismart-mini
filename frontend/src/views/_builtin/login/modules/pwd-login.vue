<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from 'vue';
import { loginModuleRecord } from '@/constants/app';
import { useAuthStore } from '@/store/modules/auth';
import { useRouterPush } from '@/hooks/common/router';
import { useFormRules, useNaiveForm } from '@/hooks/common/form';
import { localStg } from '@/utils/storage';
import { $t } from '@/locales';

defineOptions({
  name: 'PwdLogin'
});

const authStore = useAuthStore();
const isPortfolioDemo = import.meta.env.VITE_PORTFOLIO_DEMO === 'Y';
const demoUsername = import.meta.env.VITE_DEMO_USERNAME || 'demo_user';
const demoPassword = import.meta.env.VITE_DEMO_PASSWORD || '';
const backendStatus = ref<'checking' | 'warming' | 'ready' | 'error'>('checking');
let healthTimer: ReturnType<typeof setTimeout> | undefined;
const { toggleLoginModule } = useRouterPush();
const { formRef, validate } = useNaiveForm();

interface FormModel {
  userName: string;
  password: string;
  rememberMe: boolean;
}

type FormRuleModel = Pick<FormModel, 'userName' | 'password'>;

const rememberedLogin = localStg.get('rememberedLogin');

const model: FormModel = reactive({
  userName: rememberedLogin?.userName || '',
  password: rememberedLogin?.password || '',
  rememberMe: Boolean(rememberedLogin)
});

function fillDemoCredentials() {
  model.userName = demoUsername;
  model.password = demoPassword;
}

async function checkDemoBackend() {
  if (!isPortfolioDemo) return;
  backendStatus.value = 'checking';
  try {
    const baseUrl = import.meta.env.VITE_SERVICE_BASE_URL.replace(/\/$/, '');
    const response = await fetch(`${baseUrl}/demo/health`, { cache: 'no-store' });
    const payload = await response.json();
    backendStatus.value = response.ok && payload?.data?.status === 'ready' ? 'ready' : 'warming';
  } catch {
    backendStatus.value = 'warming';
  }
  if (backendStatus.value !== 'ready') {
    healthTimer = setTimeout(checkDemoBackend, 5000);
  }
}

onMounted(() => {
  if (isPortfolioDemo) {
    fillDemoCredentials();
    checkDemoBackend();
  }
});

onBeforeUnmount(() => {
  if (healthTimer) clearTimeout(healthTimer);
});

const rules = computed<Record<keyof FormRuleModel, App.Global.FormRule[]>>(() => {
  // inside computed to make locale reactive, if not apply i18n, you can define it without computed
  const { formRules } = useFormRules();

  return {
    userName: formRules.userName,
    password: formRules.pwd
  };
});

async function handleSubmit() {
  await validate();

  const success = await authStore.login(model.userName, model.password);

  if (!success) {
    return;
  }

  if (model.rememberMe) {
    localStg.set('rememberedLogin', {
      userName: model.userName,
      password: model.password
    });
  } else {
    localStg.remove('rememberedLogin');
  }
}

</script>

<template>
  <NForm ref="formRef" :model="model" :rules="rules" size="large" :show-label="false" @keyup.enter="handleSubmit">
    <NAlert v-if="isPortfolioDemo" :type="backendStatus === 'ready' ? 'success' : 'info'" class="mb-4">
      <template #header>只读 RAG 作品演示</template>
      <span v-if="backendStatus === 'ready'">服务已就绪。请勿输入个人隐私、公司机密或其他敏感信息。</span>
      <span v-else>免费后端正在唤醒，通常需要约 1 分钟，页面会自动重试。</span>
    </NAlert>
    <NFormItem path="userName">
      <NInput v-model:value="model.userName" :placeholder="$t('page.login.common.userNamePlaceholder')">
        <template #prefix>
          <icon-ant-design:user-outlined />
        </template>
      </NInput>
    </NFormItem>
    <NFormItem path="password">
      <NInput
        v-model:value="model.password"
        type="password"
        show-password-on="click"
        :placeholder="$t('page.login.common.passwordPlaceholder')"
      >
        <template #prefix>
          <icon-ant-design:key-outlined />
        </template>
      </NInput>
    </NFormItem>
    <div class="mb-6 flex-y-center justify-between">
      <NCheckbox v-model:checked="model.rememberMe">
        {{ $t('page.login.pwdLogin.rememberMe') }}
      </NCheckbox>
    </div>
    <div class="flex-col gap-6">
      <NButton
        type="primary"
        size="large"
        round
        block
        :disabled="isPortfolioDemo && backendStatus !== 'ready'"
        :loading="authStore.loginLoading"
        @click="handleSubmit"
      >
        {{ $t('page.login.common.login') }}
      </NButton>
      <NButton v-if="isPortfolioDemo" block @click="fillDemoCredentials">一键填入体验账号</NButton>
      <NButton v-else block @click="toggleLoginModule('register')">
        {{ $t(loginModuleRecord.register) }}
      </NButton>

      <span class="text-center">
        登录即代表已阅读并同意我们的
        <NButton text type="primary">用户协议</NButton>
        和
        <NButton text type="primary">隐私政策</NButton>
      </span>
    </div>
  </NForm>
</template>

<style scoped></style>
