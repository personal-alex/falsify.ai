// Base interfaces and types for wizard step validation system

export interface WizardStepValidation {
  isValid: boolean
  errors: string[]
  warnings: string[]
}

export interface WizardStepData {
  [key: string]: any
}

export interface WizardStepState {
  id: string
  isValid: boolean
  isComplete: boolean
  isDirty: boolean
  data: WizardStepData
  validation: WizardStepValidation
  lastValidated: Date | null
}

export interface WizardNavigationGuard {
  canLeave: (currentStepData: WizardStepData) => Promise<boolean> | boolean
  canEnter: (stepData: WizardStepData) => Promise<boolean> | boolean
  beforeLeave?: (currentStepData: WizardStepData) => Promise<void> | void
  afterEnter?: (stepData: WizardStepData) => Promise<void> | void
}

export interface WizardStepConfig {
  id: string
  title: string
  description: string
  component: any
  validation: WizardStepValidator
  navigationGuard?: WizardNavigationGuard
  dependencies?: string[] // IDs of steps that must be completed first
  optional?: boolean
}

export abstract class WizardStepValidator {
  abstract validate(data: WizardStepData): Promise<WizardStepValidation> | WizardStepValidation
  
  protected createValidation(
    isValid: boolean, 
    errors: string[] = [], 
    warnings: string[] = []
  ): WizardStepValidation {
    return { isValid, errors, warnings }
  }
  
  protected validateRequired(value: any, fieldName: string): string | null {
    if (value === null || value === undefined || value === '') {
      return `${fieldName} is required`
    }
    return null
  }
  
  protected validateMinLength(value: string, minLength: number, fieldName: string): string | null {
    if (value && value.length < minLength) {
      return `${fieldName} must be at least ${minLength} characters`
    }
    return null
  }
  
  protected validateArray(value: any[], minItems: number, fieldName: string): string | null {
    if (!Array.isArray(value) || value.length < minItems) {
      return `${fieldName} must contain at least ${minItems} items`
    }
    return null
  }
}

// Specific validators for each step
export class ArticleSelectionValidator extends WizardStepValidator {
  validate(data: WizardStepData): WizardStepValidation {
    const errors: string[] = []
    const warnings: string[] = []
    
    // Validate selected articles
    const selectedArticles = data.selectedArticles || []
    
    if (!Array.isArray(selectedArticles) || selectedArticles.length === 0) {
      errors.push('At least one article must be selected for analysis')
    }
    
    // Warning for large selections
    if (selectedArticles.length > 100) {
      warnings.push('Large article selections may take longer to process')
    }
    
    // Validate article data integrity
    const invalidArticles = selectedArticles.filter((article: any) => 
      !article.id || !article.title || !article.text
    )
    
    if (invalidArticles.length > 0) {
      errors.push(`${invalidArticles.length} selected articles have missing required data`)
    }
    
    return this.createValidation(errors.length === 0, errors, warnings)
  }
}

export class AnalysisExecutionValidator extends WizardStepValidator {
  validate(data: WizardStepData): WizardStepValidation {
    const errors: string[] = []
    const warnings: string[] = []
    
    // Validate analysis job
    const currentJob = data.currentJob
    
    if (!currentJob) {
      errors.push('Analysis job must be started')
      return this.createValidation(false, errors, warnings)
    }
    
    // Check job status
    if (currentJob.status === 'FAILED') {
      errors.push('Analysis job failed and must be retried')
    } else if (currentJob.status === 'CANCELLED') {
      errors.push('Analysis job was cancelled and must be restarted')
    } else if (currentJob.status !== 'COMPLETED') {
      errors.push('Analysis job must be completed before proceeding')
    }
    
    // Validate job results
    if (currentJob.status === 'COMPLETED') {
      if (!currentJob.processedArticles || currentJob.processedArticles === 0) {
        warnings.push('No articles were processed in the analysis')
      }
      
      if (!currentJob.predictionsFound || currentJob.predictionsFound === 0) {
        warnings.push('No predictions were found in the analyzed articles')
      }
    }
    
    return this.createValidation(errors.length === 0, errors, warnings)
  }
}

export class AnalysisResultsValidator extends WizardStepValidator {
  validate(data: WizardStepData): WizardStepValidation {
    const errors: string[] = []
    const warnings: string[] = []
    
    // Validate results data
    const results = data.results || []
    
    if (!Array.isArray(results)) {
      errors.push('Invalid results data format')
      return this.createValidation(false, errors, warnings)
    }
    
    // Check for empty results
    if (results.length === 0) {
      warnings.push('No prediction results to display')
    }
    
    // Validate result data integrity
    const invalidResults = results.filter(result => 
      !result.id || !result.predictionText || !result.article
    )
    
    if (invalidResults.length > 0) {
      errors.push(`${invalidResults.length} results have missing required data`)
    }
    
    return this.createValidation(errors.length === 0, errors, warnings)
  }
}

// Navigation guards for each step
export class ArticleSelectionNavigationGuard implements WizardNavigationGuard {
  canLeave(currentStepData: WizardStepData): boolean {
    const selectedArticles = currentStepData.selectedArticles || []
    return selectedArticles.length > 0
  }
  
  canEnter(): boolean {
    return true // Always can enter article selection
  }
  
  async beforeLeave(currentStepData: WizardStepData): Promise<void> {
    // Save selection state
    const selectedArticles = currentStepData.selectedArticles || []
    console.log(`Leaving article selection with ${selectedArticles.length} articles selected`)
  }
}

export class AnalysisExecutionNavigationGuard implements WizardNavigationGuard {
  canLeave(currentStepData: WizardStepData): boolean {
    const currentJob = currentStepData.currentJob
    return currentJob && currentJob.status === 'COMPLETED'
  }
  
  canEnter(stepData: WizardStepData): boolean {
    const selectedArticles = stepData.selectedArticles || []
    return selectedArticles.length > 0
  }
  
  async beforeLeave(currentStepData: WizardStepData): Promise<void> {
    // Ensure analysis is complete
    const currentJob = currentStepData.currentJob
    if (currentJob && currentJob.status === 'RUNNING') {
      throw new Error('Cannot leave while analysis is running')
    }
  }
}

export class AnalysisResultsNavigationGuard implements WizardNavigationGuard {
  canLeave(): boolean {
    return true // Can always leave results step
  }
  
  canEnter(stepData: WizardStepData): boolean {
    const currentJob = stepData.currentJob
    return currentJob && currentJob.status === 'COMPLETED'
  }
}

// Step configuration factory
export class WizardStepConfigFactory {
  static createArticleSelectionStep(): WizardStepConfig {
    return {
      id: 'article-selection',
      title: 'Select Articles',
      description: 'Choose articles for analysis',
      component: null, // Will be set by the wizard
      validation: new ArticleSelectionValidator(),
      navigationGuard: new ArticleSelectionNavigationGuard(),
      dependencies: [],
      optional: false
    }
  }
  
  static createAnalysisExecutionStep(): WizardStepConfig {
    return {
      id: 'analysis-execution',
      title: 'Run Analysis',
      description: 'Execute prediction analysis',
      component: null, // Will be set by the wizard
      validation: new AnalysisExecutionValidator(),
      navigationGuard: new AnalysisExecutionNavigationGuard(),
      dependencies: ['article-selection'],
      optional: false
    }
  }
  
  static createAnalysisResultsStep(): WizardStepConfig {
    return {
      id: 'analysis-results',
      title: 'View Results',
      description: 'Review analysis results',
      component: null, // Will be set by the wizard
      validation: new AnalysisResultsValidator(),
      navigationGuard: new AnalysisResultsNavigationGuard(),
      dependencies: ['article-selection', 'analysis-execution'],
      optional: false
    }
  }
}

// Wizard state manager
export class WizardStateManager {
  private static readonly STORAGE_KEY = 'prediction-analysis-wizard-state'
  private static readonly MAX_AGE = 24 * 60 * 60 * 1000 // 24 hours
  
  static saveState(state: any): void {
    try {
      const stateData = {
        ...state,
        timestamp: Date.now()
      }
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(stateData))
    } catch (error) {
      console.warn('Failed to save wizard state:', error)
    }
  }
  
  static loadState(): any | null {
    try {
      const saved = localStorage.getItem(this.STORAGE_KEY)
      if (!saved) return null
      
      const data = JSON.parse(saved)
      
      // Check if state is not too old
      if (Date.now() - data.timestamp > this.MAX_AGE) {
        this.clearState()
        return null
      }
      
      return data
    } catch (error) {
      console.warn('Failed to load wizard state:', error)
      return null
    }
  }
  
  static clearState(): void {
    try {
      localStorage.removeItem(this.STORAGE_KEY)
    } catch (error) {
      console.warn('Failed to clear wizard state:', error)
    }
  }
  
  static isStateExpired(timestamp: number): boolean {
    return Date.now() - timestamp > this.MAX_AGE
  }
}

// Utility functions for step validation
export class WizardValidationUtils {
  static async validateAllSteps(
    steps: Array<{ id: string; validation: WizardStepValidator }>, 
    stepStates: Map<string, WizardStepState>
  ): Promise<Map<string, WizardStepValidation>> {
    const validations = new Map<string, WizardStepValidation>()
    
    for (const step of steps) {
      const stepState = stepStates.get(step.id)
      if (stepState) {
        const validation = await step.validation.validate(stepState.data)
        validations.set(step.id, validation)
      }
    }
    
    return validations
  }
  
  static checkStepDependencies(
    stepId: string, 
    steps: Array<{ id: string; dependencies: string[] }>, 
    stepStates: Map<string, WizardStepState>
  ): boolean {
    const step = steps.find(s => s.id === stepId)
    if (!step || !step.dependencies) return true
    
    return step.dependencies.every(depId => {
      const depState = stepStates.get(depId)
      return depState && depState.isComplete
    })
  }
  
  static getNextAvailableStep(
    currentStepId: string, 
    steps: Array<{ id: string; dependencies: string[] }>, 
    stepStates: Map<string, WizardStepState>
  ): string | null {
    const currentIndex = steps.findIndex(s => s.id === currentStepId)
    if (currentIndex === -1) return null
    
    for (let i = currentIndex + 1; i < steps.length; i++) {
      const step = steps[i]
      if (this.checkStepDependencies(step.id, steps, stepStates)) {
        return step.id
      }
    }
    
    return null
  }
  
  static getPreviousAvailableStep(
    currentStepId: string, 
    steps: Array<{ id: string }>
  ): string | null {
    const currentIndex = steps.findIndex(s => s.id === currentStepId)
    if (currentIndex <= 0) return null
    
    return steps[currentIndex - 1].id
  }
}