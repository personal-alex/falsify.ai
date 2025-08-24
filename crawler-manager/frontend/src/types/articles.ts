// Article and prediction related types

export interface Author {
  id: number
  name: string
  avatarUrl?: string
  articleCount?: number
}

export interface Article {
  id: number
  title: string
  url: string
  text: string
  crawlerSource: string
  createdAt: string
  author?: Author
  predictions?: Prediction[]
}

export interface ArticleFilters {
  authorId?: number | null
  titleSearch?: string
  dateRange?: Date[] | null
  crawlerSource?: string | null
}

export interface Prediction {
  id: number
  predictionText: string
  rating: number
  confidenceScore: number
  context?: string
  extractedAt: string
}

