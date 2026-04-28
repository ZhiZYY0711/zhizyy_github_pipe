<script setup lang="ts">
import type { LoginErrorState, LoginFormModel } from '../../modules/auth/types'

const props = withDefaults(
  defineProps<{
    form: {
      eyebrow: string
      title: string
      note: string
      accountLabel: string
      accountPlaceholder: string
      passwordLabel: string
      passwordPlaceholder: string
      captchaLabel: string
      captchaPlaceholder: string
      actionText: string
    }
    modelValue: LoginFormModel
    captchaValue: string
    metaItems: readonly string[]
    errors: LoginErrorState
    isSubmitting?: boolean
  }>(),
  {
    isSubmitting: false,
  },
)

const emit = defineEmits<{
  (event: 'update:modelValue', value: LoginFormModel): void
  (event: 'submit'): void
  (event: 'refreshCaptcha'): void
}>()

function updateField(field: keyof LoginFormModel, event: Event) {
  const target = event.target as HTMLInputElement

  emit('update:modelValue', {
    ...props.modelValue,
    [field]: target.value,
  })
}
</script>

<template>
  <section class="login-access">
    <article class="access-card">
      <p class="eyebrow">{{ form.eyebrow }}</p>
      <h2 class="access-card__title">{{ form.title }}</h2>
      <p class="access-card__note">{{ form.note }}</p>

      <form class="access-form" @submit.prevent="emit('submit')">
        <label class="field" :class="{ 'is-invalid': errors.username }">
          <span>{{ form.accountLabel }}</span>
          <input
            type="text"
            autocomplete="username"
            :placeholder="form.accountPlaceholder"
            :value="modelValue.username"
            :aria-invalid="Boolean(errors.username)"
            @input="updateField('username', $event)"
          />
        </label>
        <p v-if="errors.username" class="field-error">{{ errors.username }}</p>

        <label class="field" :class="{ 'is-invalid': errors.password }">
          <span>{{ form.passwordLabel }}</span>
          <input
            type="password"
            autocomplete="current-password"
            :placeholder="form.passwordPlaceholder"
            :value="modelValue.password"
            :aria-invalid="Boolean(errors.password)"
            @input="updateField('password', $event)"
          />
        </label>
        <p v-if="errors.password" class="field-error">{{ errors.password }}</p>

        <div class="field-row">
          <div>
            <label class="field" :class="{ 'is-invalid': errors.code }">
              <span>{{ form.captchaLabel }}</span>
              <input
                type="text"
                inputmode="text"
                autocomplete="one-time-code"
                :placeholder="form.captchaPlaceholder"
                :value="modelValue.code"
                maxlength="6"
                :aria-invalid="Boolean(errors.code)"
                @input="updateField('code', $event)"
              />
            </label>
            <p v-if="errors.code" class="field-error">{{ errors.code }}</p>
          </div>

          <button
            type="button"
            class="captcha captcha-button"
            @click="emit('refreshCaptcha')"
          >
            {{ captchaValue }}
          </button>
        </div>

        <p v-if="errors.form" class="form-error">{{ errors.form }}</p>

        <button class="primary-action" type="submit" :disabled="isSubmitting">
          {{ isSubmitting ? 'Signing in...' : form.actionText }}
        </button>
      </form>

      <div class="access-meta">
        <span v-for="item in metaItems" :key="item">{{ item }}</span>
      </div>
    </article>
  </section>
</template>

<style scoped>
.access-form {
  margin-top: 18px;
}

.field {
  margin-top: 0;
}

.field.is-invalid input {
  border-color: rgba(231, 103, 45, 0.52);
  box-shadow: 0 0 0 1px rgba(231, 103, 45, 0.16);
}

.field-error,
.form-error {
  margin: 8px 0 0;
  font-size: 12px;
  line-height: 1.4;
  color: #f58c55;
}

.field-row {
  align-items: start;
  margin-top: 18px;
}

.captcha-button {
  border: 1px solid rgba(231, 103, 45, 0.28);
  cursor: pointer;
}

.primary-action:disabled {
  opacity: 0.72;
  cursor: wait;
}
</style>
