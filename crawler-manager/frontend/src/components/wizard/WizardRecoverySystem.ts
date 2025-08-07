// Wizard state recovery and persistence system
import { 
  type WizardStepState
} from './WizardStepBase'

export interface WizardRecoveryData {
  currentStep: number
  stepStates: Map<string, WizardStepState>
  wizardState: any
  timestamp: number
  version: string
  sessionId: string
}

export interface WizardRecoveryOptions {
  maxAge?: number // Maximum age in milliseconds
  enableSessionRecovery?: boolean
  enableCrossTabRecovery?: boolean
  compressionEnabled?: boolean
  encryptionEnabled?: boolean
}

export class WizardRecoverySystem {
  private static readonly CURRENT_VERSION = '1.0.0'
  private static readonly SESSION_KEY = 'wizard-session-id'
  private static readonly RECOVERY_KEY = 'wizard-recovery-data'
  private static readonly DEFAULT_MAX_AGE = 24 * 60 * 60 * 1000 // 24 hours
  
  private options: Required<WizardRecoveryOptions>
  private sessionId: string
  
  constructor(options: WizardRecoveryOptions = {}) {
    this.options = {
      maxAge: options.maxAge || WizardRecoverySystem.DEFAULT_MAX_AGE,
      enableSessionRecovery: options.enableSessionRecovery ?? true,
      enableCrossTabRecovery: options.enableCrossTabRecovery ?? false,
      compressionEnabled: options.compressionEnabled ?? false,
      encryptionEnabled: options.encryptionEnabled ?? false
    }
    
    this.sessionId = this.generateSessionId()
  }
  
  // Generate unique session ID
  private generateSessionId(): string {
    return `wizard-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`
  }
  
  // Save wizard state with recovery data
  async saveRecoveryState(
    currentStep: number,
    stepStates: Map<string, WizardStepState>,
    wizardState: any
  ): Promise<void> {
    try {
      const recoveryData: WizardRecoveryData = {
        currentStep,
        stepStates,
        wizardState,
        timestamp: Date.now(),
        version: WizardRecoverySystem.CURRENT_VERSION,
        sessionId: this.sessionId
      }
      
      // Serialize the data
      const serializedData = await this.serializeRecoveryData(recoveryData)
      
      // Save to localStorage
      if (this.options.enableSessionRecovery) {
        localStorage.setItem(WizardRecoverySystem.RECOVERY_KEY, serializedData)
        sessionStorage.setItem(WizardRecoverySystem.SESSION_KEY, this.sessionId)
      }
      
      // Save to sessionStorage for cross-tab recovery
      if (this.options.enableCrossTabRecovery) {
        sessionStorage.setItem(WizardRecoverySystem.RECOVERY_KEY, serializedData)
      }
      
    } catch (error) {
      console.warn('Failed to save wizard recovery state:', error)
    }
  }
  
  // Load wizard state from recovery data
  async loadRecoveryState(): Promise<WizardRecoveryData | null> {
    try {
      let serializedData: string | null = null
      
      // Try to load from localStorage first
      if (this.options.enableSessionRecovery) {
        serializedData = localStorage.getItem(WizardRecoverySystem.RECOVERY_KEY)
      }
      
      // Fallback to sessionStorage for cross-tab recovery
      if (!serializedData && this.options.enableCrossTabRecovery) {
        serializedData = sessionStorage.getItem(WizardRecoverySystem.RECOVERY_KEY)
      }
      
      if (!serializedData) {
        return null
      }
      
      // Deserialize the data
      const recoveryData = await this.deserializeRecoveryData(serializedData)
      
      // Validate recovery data
      if (!this.isRecoveryDataValid(recoveryData)) {
        this.clearRecoveryState()
        return null
      }
      
      return recoveryData
      
    } catch (error) {
      console.warn('Failed to load wizard recovery state:', error)
      this.clearRecoveryState()
      return null
    }
  }
  
  // Clear recovery state
  clearRecoveryState(): void {
    try {
      localStorage.removeItem(WizardRecoverySystem.RECOVERY_KEY)
      sessionStorage.removeItem(WizardRecoverySystem.RECOVERY_KEY)
      sessionStorage.removeItem(WizardRecoverySystem.SESSION_KEY)
    } catch (error) {
      console.warn('Failed to clear wizard recovery state:', error)
    }
  }
  
  // Check if recovery is available
  async isRecoveryAvailable(): Promise<boolean> {
    const recoveryData = await this.loadRecoveryState()
    return recoveryData !== null
  }
  
  // Get recovery summary for user display
  async getRecoverySummary(): Promise<{
    stepName: string
    lastSaved: Date
    dataSize: string
    isExpired: boolean
  } | null> {
    const recoveryData = await this.loadRecoveryState()
    
    if (!recoveryData) {
      return null
    }
    
    const stepNames = ['Article Selection', 'Analysis Execution', 'Analysis Results']
    const stepName = stepNames[recoveryData.currentStep - 1] || 'Unknown Step'
    const lastSaved = new Date(recoveryData.timestamp)
    const isExpired = Date.now() - recoveryData.timestamp > this.options.maxAge
    
    // Calculate approximate data size
    const dataSize = this.calculateDataSize(recoveryData)
    
    return {
      stepName,
      lastSaved,
      dataSize,
      isExpired
    }
  }
  
  // Serialize recovery data
  private async serializeRecoveryData(data: WizardRecoveryData): Promise<string> {
    try {
      // Convert Map to array for serialization
      const serializable = {
        ...data,
        stepStates: Array.from(data.stepStates.entries()).map(([id, state]) => [
          id,
          {
            ...state,
            lastValidated: state.lastValidated?.toISOString()
          }
        ])
      }
      
      let serialized = JSON.stringify(serializable)
      
      // Apply compression if enabled
      if (this.options.compressionEnabled) {
        serialized = await this.compressData(serialized)
      }
      
      // Apply encryption if enabled
      if (this.options.encryptionEnabled) {
        serialized = await this.encryptData(serialized)
      }
      
      return serialized
      
    } catch (error) {
      throw new Error(`Failed to serialize recovery data: ${error}`)
    }
  }
  
  // Deserialize recovery data
  private async deserializeRecoveryData(serializedData: string): Promise<WizardRecoveryData> {
    try {
      let data = serializedData
      
      // Apply decryption if enabled
      if (this.options.encryptionEnabled) {
        data = await this.decryptData(data)
      }
      
      // Apply decompression if enabled
      if (this.options.compressionEnabled) {
        data = await this.decompressData(data)
      }
      
      const parsed = JSON.parse(data)
      
      // Convert array back to Map
      const stepStates = new Map<string, WizardStepState>()
      if (parsed.stepStates) {
        parsed.stepStates.forEach(([id, state]: [string, any]) => {
          stepStates.set(id, {
            ...state,
            lastValidated: state.lastValidated ? new Date(state.lastValidated) : null
          })
        })
      }
      
      return {
        ...parsed,
        stepStates
      }
      
    } catch (error) {
      throw new Error(`Failed to deserialize recovery data: ${error}`)
    }
  }
  
  // Validate recovery data
  private isRecoveryDataValid(data: WizardRecoveryData): boolean {
    // Check version compatibility
    if (data.version !== WizardRecoverySystem.CURRENT_VERSION) {
      console.warn(`Recovery data version mismatch: ${data.version} vs ${WizardRecoverySystem.CURRENT_VERSION}`)
      return false
    }
    
    // Check age
    if (Date.now() - data.timestamp > this.options.maxAge) {
      console.warn('Recovery data is too old')
      return false
    }
    
    // Check required fields
    if (!data.currentStep || !data.stepStates || !data.wizardState) {
      console.warn('Recovery data is missing required fields')
      return false
    }
    
    // Check step bounds
    if (data.currentStep < 1 || data.currentStep > 3) {
      console.warn('Recovery data has invalid current step')
      return false
    }
    
    return true
  }
  
  // Calculate approximate data size
  private calculateDataSize(data: WizardRecoveryData): string {
    try {
      const serialized = JSON.stringify(data)
      const bytes = new Blob([serialized]).size
      
      if (bytes < 1024) {
        return `${bytes} B`
      } else if (bytes < 1024 * 1024) {
        return `${Math.round(bytes / 1024)} KB`
      } else {
        return `${Math.round(bytes / (1024 * 1024))} MB`
      }
    } catch {
      return 'Unknown'
    }
  }
  
  // Compression methods (placeholder implementations)
  private async compressData(data: string): Promise<string> {
    // In a real implementation, you might use a compression library
    // For now, just return the data as-is
    return data
  }
  
  private async decompressData(data: string): Promise<string> {
    // In a real implementation, you might use a compression library
    // For now, just return the data as-is
    return data
  }
  
  // Encryption methods (placeholder implementations)
  private async encryptData(data: string): Promise<string> {
    // In a real implementation, you might use the Web Crypto API
    // For now, just return the data as-is
    return data
  }
  
  private async decryptData(data: string): Promise<string> {
    // In a real implementation, you might use the Web Crypto API
    // For now, just return the data as-is
    return data
  }
}

// Recovery dialog component data
export interface WizardRecoveryDialogData {
  show: boolean
  summary: {
    stepName: string
    lastSaved: Date
    dataSize: string
    isExpired: boolean
  } | null
  loading: boolean
}

// Recovery manager for handling recovery UI
export class WizardRecoveryManager {
  private recoverySystem: WizardRecoverySystem
  
  constructor(options: WizardRecoveryOptions = {}) {
    this.recoverySystem = new WizardRecoverySystem(options)
  }
  
  // Check if recovery should be offered to user
  async shouldOfferRecovery(): Promise<boolean> {
    return await this.recoverySystem.isRecoveryAvailable()
  }
  
  // Get recovery dialog data
  async getRecoveryDialogData(): Promise<WizardRecoveryDialogData> {
    const summary = await this.recoverySystem.getRecoverySummary()
    
    return {
      show: summary !== null && !summary.isExpired,
      summary,
      loading: false
    }
  }
  
  // Accept recovery
  async acceptRecovery(): Promise<WizardRecoveryData | null> {
    return await this.recoverySystem.loadRecoveryState()
  }
  
  // Decline recovery
  async declineRecovery(): Promise<void> {
    this.recoverySystem.clearRecoveryState()
  }
  
  // Save current state
  async saveState(
    currentStep: number,
    stepStates: Map<string, WizardStepState>,
    wizardState: any
  ): Promise<void> {
    await this.recoverySystem.saveRecoveryState(currentStep, stepStates, wizardState)
  }
  
  // Clear recovery state
  clearState(): void {
    this.recoverySystem.clearRecoveryState()
  }
}

// Auto-save manager for periodic state saving
export class WizardAutoSaveManager {
  private recoveryManager: WizardRecoveryManager
  private autoSaveInterval: NodeJS.Timeout | null = null
  private isDirty = false
  private saveIntervalMs: number
  
  constructor(
    recoveryManager: WizardRecoveryManager,
    saveIntervalMs = 30000 // 30 seconds
  ) {
    this.recoveryManager = recoveryManager
    this.saveIntervalMs = saveIntervalMs
  }
  
  // Start auto-save
  startAutoSave(
    getCurrentStep: () => number,
    getStepStates: () => Map<string, WizardStepState>,
    getWizardState: () => any
  ): void {
    if (this.autoSaveInterval) {
      this.stopAutoSave()
    }
    
    this.autoSaveInterval = setInterval(async () => {
      if (this.isDirty) {
        try {
          await this.recoveryManager.saveState(
            getCurrentStep(),
            getStepStates(),
            getWizardState()
          )
          this.isDirty = false
        } catch (error) {
          console.warn('Auto-save failed:', error)
        }
      }
    }, this.saveIntervalMs)
  }
  
  // Stop auto-save
  stopAutoSave(): void {
    if (this.autoSaveInterval) {
      clearInterval(this.autoSaveInterval)
      this.autoSaveInterval = null
    }
  }
  
  // Mark state as dirty (needs saving)
  markDirty(): void {
    this.isDirty = true
  }
  
  // Force immediate save
  async forceSave(
    getCurrentStep: () => number,
    getStepStates: () => Map<string, WizardStepState>,
    getWizardState: () => any
  ): Promise<void> {
    await this.recoveryManager.saveState(
      getCurrentStep(),
      getStepStates(),
      getWizardState()
    )
    this.isDirty = false
  }
}