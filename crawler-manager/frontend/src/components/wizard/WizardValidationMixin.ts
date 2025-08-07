// Vue composition function for wizard step validation
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { 
  WizardStepValidator, 
  type WizardStepValidation, 
  type WizardStepData 
} from './WizardStepBase'

export interface UseWizardStepValidationOptions {
  validator: WizardStepValidator
  debounceMs?: number
  validateOnMount?: boolean
  autoEmitEvents?: boolean
}

export function useWizardStepValidation(
  stepData: any,
  options: UseWizardStepValidationOptions,
  emit?: any
) {
  const {
    validator,
    debounceMs = 300,
    validateOnMount = true,
    autoEmitEvents = true
  } = options

  // Reactive state
  const validation = ref<WizardStepValidation>({
    isValid: false,
    errors: [],
    warnings: []
  })
  
  const isValidating = ref(false)
  const lastValidated = ref<Date | null>(null)
  
  // Debounce timer
  let validationTimer: NodeJS.Timeout | null = null

  // Computed properties
  const isValid = computed(() => validation.value.isValid)
  const hasErrors = computed(() => validation.value.errors.length > 0)
  const hasWarnings = computed(() => validation.value.warnings.length > 0)
  const errorMessages = computed(() => validation.value.errors)
  const warningMessages = computed(() => validation.value.warnings)

  // Methods
  const validateStep = async (immediate = false): Promise<WizardStepValidation> => {
    if (validationTimer) {
      clearTimeout(validationTimer)
      validationTimer = null
    }

    if (!immediate && debounceMs > 0) {
      return new Promise((resolve) => {
        validationTimer = setTimeout(async () => {
          const result = await performValidation()
          resolve(result)
        }, debounceMs)
      })
    }

    return await performValidation()
  }

  const performValidation = async (): Promise<WizardStepValidation> => {
    isValidating.value = true

    try {
      const result = await validator.validate(stepData.value || {})
      validation.value = result
      lastValidated.value = new Date()

      // Emit events if enabled
      if (autoEmitEvents && emit) {
        emit('step-valid', result.isValid)
        
        if (result.errors.length > 0) {
          emit('step-errors', result.errors)
        }
        
        if (result.warnings.length > 0) {
          emit('step-warnings', result.warnings)
        }
      }

      return result
    } catch (error) {
      console.error('Validation error:', error)
      const errorResult: WizardStepValidation = {
        isValid: false,
        errors: ['Validation failed due to an internal error'],
        warnings: []
      }
      validation.value = errorResult
      return errorResult
    } finally {
      isValidating.value = false
    }
  }

  const clearValidation = () => {
    validation.value = {
      isValid: false,
      errors: [],
      warnings: []
    }
    lastValidated.value = null
    
    if (autoEmitEvents && emit) {
      emit('step-valid', false)
    }
  }

  const forceValidation = () => validateStep(true)

  // Watch for data changes
  watch(
    stepData,
    () => {
      validateStep()
    },
    { deep: true }
  )

  // Lifecycle
  onMounted(() => {
    if (validateOnMount) {
      validateStep(true)
    }
  })

  onUnmounted(() => {
    if (validationTimer) {
      clearTimeout(validationTimer)
    }
  })

  return {
    // State
    validation: computed(() => validation.value),
    isValidating: computed(() => isValidating.value),
    lastValidated: computed(() => lastValidated.value),
    
    // Computed
    isValid,
    hasErrors,
    hasWarnings,
    errorMessages,
    warningMessages,
    
    // Methods
    validateStep,
    clearValidation,
    forceValidation
  }
}

// Validation message formatter
export class ValidationMessageFormatter {
  static formatError(error: string): string {
    return `❌ ${error}`
  }
  
  static formatWarning(warning: string): string {
    return `⚠️ ${warning}`
  }
  
  static formatErrors(errors: string[]): string[] {
    return errors.map(this.formatError)
  }
  
  static formatWarnings(warnings: string[]): string[] {
    return warnings.map(this.formatWarning)
  }
  
  static createSummary(validation: WizardStepValidation): string {
    const parts: string[] = []
    
    if (validation.errors.length > 0) {
      parts.push(`${validation.errors.length} error${validation.errors.length > 1 ? 's' : ''}`)
    }
    
    if (validation.warnings.length > 0) {
      parts.push(`${validation.warnings.length} warning${validation.warnings.length > 1 ? 's' : ''}`)
    }
    
    if (parts.length === 0) {
      return validation.isValid ? 'Valid' : 'Invalid'
    }
    
    return parts.join(', ')
  }
}

// Validation rules library
export class ValidationRules {
  static required(fieldName: string) {
    return (value: any): string | null => {
      if (value === null || value === undefined || value === '' || 
          (Array.isArray(value) && value.length === 0)) {
        return `${fieldName} is required`
      }
      return null
    }
  }
  
  static minLength(fieldName: string, minLength: number) {
    return (value: string): string | null => {
      if (value && value.length < minLength) {
        return `${fieldName} must be at least ${minLength} characters`
      }
      return null
    }
  }
  
  static maxLength(fieldName: string, maxLength: number) {
    return (value: string): string | null => {
      if (value && value.length > maxLength) {
        return `${fieldName} must be no more than ${maxLength} characters`
      }
      return null
    }
  }
  
  static minItems(fieldName: string, minItems: number) {
    return (value: any[]): string | null => {
      if (!Array.isArray(value) || value.length < minItems) {
        return `${fieldName} must contain at least ${minItems} item${minItems > 1 ? 's' : ''}`
      }
      return null
    }
  }
  
  static maxItems(fieldName: string, maxItems: number) {
    return (value: any[]): string | null => {
      if (Array.isArray(value) && value.length > maxItems) {
        return `${fieldName} must contain no more than ${maxItems} item${maxItems > 1 ? 's' : ''}`
      }
      return null
    }
  }
  
  static pattern(fieldName: string, pattern: RegExp, message?: string) {
    return (value: string): string | null => {
      if (value && !pattern.test(value)) {
        return message || `${fieldName} format is invalid`
      }
      return null
    }
  }
  
  static range(fieldName: string, min: number, max: number) {
    return (value: number): string | null => {
      if (typeof value === 'number' && (value < min || value > max)) {
        return `${fieldName} must be between ${min} and ${max}`
      }
      return null
    }
  }
  
  static custom(_fieldName: string, validator: (value: any) => boolean, message: string) {
    return (value: any): string | null => {
      if (!validator(value)) {
        return message
      }
      return null
    }
  }
}

// Composite validator for complex validation scenarios
export class CompositeValidator extends WizardStepValidator {
  private rules: Array<(data: WizardStepData) => string | null> = []
  private warnings: Array<(data: WizardStepData) => string | null> = []
  
  addRule(rule: (data: WizardStepData) => string | null): this {
    this.rules.push(rule)
    return this
  }
  
  addWarning(warning: (data: WizardStepData) => string | null): this {
    this.warnings.push(warning)
    return this
  }
  
  validate(data: WizardStepData): WizardStepValidation {
    const errors: string[] = []
    const warnings: string[] = []
    
    // Run error rules
    for (const rule of this.rules) {
      const error = rule(data)
      if (error) {
        errors.push(error)
      }
    }
    
    // Run warning rules
    for (const warning of this.warnings) {
      const warn = warning(data)
      if (warn) {
        warnings.push(warn)
      }
    }
    
    return this.createValidation(errors.length === 0, errors, warnings)
  }
}

// Field validator for individual field validation
export class FieldValidator {
  private fieldName: string
  private rules: Array<(value: any) => string | null> = []
  
  constructor(fieldName: string) {
    this.fieldName = fieldName
  }
  
  required(): this {
    this.rules.push(ValidationRules.required(this.fieldName))
    return this
  }
  
  minLength(length: number): this {
    this.rules.push(ValidationRules.minLength(this.fieldName, length))
    return this
  }
  
  maxLength(length: number): this {
    this.rules.push(ValidationRules.maxLength(this.fieldName, length))
    return this
  }
  
  minItems(count: number): this {
    this.rules.push(ValidationRules.minItems(this.fieldName, count))
    return this
  }
  
  maxItems(count: number): this {
    this.rules.push(ValidationRules.maxItems(this.fieldName, count))
    return this
  }
  
  pattern(pattern: RegExp, message?: string): this {
    this.rules.push(ValidationRules.pattern(this.fieldName, pattern, message))
    return this
  }
  
  range(min: number, max: number): this {
    this.rules.push(ValidationRules.range(this.fieldName, min, max))
    return this
  }
  
  custom(validator: (value: any) => boolean, message: string): this {
    this.rules.push(ValidationRules.custom(this.fieldName, validator, message))
    return this
  }
  
  validate(value: any): string[] {
    const errors: string[] = []
    
    for (const rule of this.rules) {
      const error = rule(value)
      if (error) {
        errors.push(error)
      }
    }
    
    return errors
  }
}

// Validation builder for creating complex validators
export class ValidationBuilder {
  static field(fieldName: string): FieldValidator {
    return new FieldValidator(fieldName)
  }
  
  static composite(): CompositeValidator {
    return new CompositeValidator()
  }
}