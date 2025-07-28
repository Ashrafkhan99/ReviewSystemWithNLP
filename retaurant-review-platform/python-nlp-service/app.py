from flask import Flask, request, jsonify
from flask_cors import CORS
from transformers import pipeline
import logging
import os
import threading
from datetime import datetime
from queue import Queue

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = Flask(__name__)
CORS(app)

# Global variables for model management
sentiment_analyzer = None
model_ready = False
model_loading = False
model_error = None
initialization_queue = Queue()

class ModelLoader(threading.Thread):
    """Background thread for loading the ML model"""
    def __init__(self):
        super(ModelLoader, self).__init__()
        self.daemon = True  # Dies when main thread dies

    def run(self):
        """Load the sentiment analysis model in background"""
        global sentiment_analyzer, model_ready, model_loading, model_error

        try:
            model_loading = True
            logger.info("Starting background model loading...")

            try:
                logger.info("Loading primary model: cardiffnlp/twitter-roberta-base-sentiment-latest")
                sentiment_analyzer = pipeline(
                    "sentiment-analysis",
                    model="cardiffnlp/twitter-roberta-base-sentiment-latest",
                    return_all_scores=True
                )
                logger.info("Primary model loaded successfully!")

            except Exception as e:
                logger.warning(f"Primary model failed, using fallback: {str(e)}")
                sentiment_analyzer = pipeline(
                    "sentiment-analysis",
                    model="distilbert-base-uncased-finetuned-sst-2-english"
                )
                logger.info("Fallback model loaded successfully!")

            model_ready = True
            model_loading = False
            logger.info("Model initialization completed!")

        except Exception as e:
            model_error = str(e)
            model_loading = False
            logger.error(f"Fatal error loading model: {str(e)}")

def initialize_model_async():
    """Start background model loading"""
    if not model_loading and not model_ready:
        loader = ModelLoader()
        loader.start()
        logger.info("Model loading started in background thread")

@app.route('/health', methods=['GET'])
def health_check():
    global model_ready, model_loading, model_error

    status_info = {
        'status': 'healthy',
        'timestamp': datetime.now().isoformat(),
        'service': 'nlp-sentiment-analysis',
        'model_ready': model_ready,
        'model_loading': model_loading
    }

    if model_error:
        status_info['model_error'] = model_error
        status_info['status'] = 'degraded'

    if model_ready:
        return jsonify(status_info), 200
    elif model_loading:
        status_info['message'] = 'Model is loading, please wait...'
        return jsonify(status_info), 503
    else:
        status_info['status'] = 'unhealthy'
        status_info['message'] = 'Model failed to load'
        return jsonify(status_info), 503

@app.route('/ready', methods=['GET'])
def readiness_check():
    if model_ready:
        return jsonify({
            'status': 'ready',
            'model_ready': True,
            'timestamp': datetime.now().isoformat()
        }), 200
    else:
        return jsonify({
            'status': 'not_ready',
            'model_ready': False,
            'model_loading': model_loading,
            'timestamp': datetime.now().isoformat()
        }), 503

@app.route('/analyze', methods=['POST'])
def analyze_sentiment():
    global sentiment_analyzer, model_ready

    try:
        if not model_ready:
            return jsonify({
                'error': 'Model not ready yet. Please wait for initialization.',
                'model_loading': model_loading,
                'retry_after': 10
            }), 503

        if not request.is_json:
            return jsonify({'error': 'Content-Type must be application/json'}), 400

        data = request.get_json()
        if not data or 'text' not in data:
            return jsonify({'error': 'Missing required field: text'}), 400

        review_text = data['text'].strip()
        if not review_text:
            return jsonify({'error': 'Text cannot be empty'}), 400

        logger.info(f"Analyzing sentiment for text: {review_text[:50]}...")
        result = sentiment_analyzer(review_text)

        if isinstance(result[0], list):
            sentiment_data = process_detailed_scores(result[0])
        else:
            sentiment_data = process_simple_score(result[0])

        response = {
            'sentiment': sentiment_data['label'],
            'confidence': sentiment_data['confidence'],
            'score': sentiment_data['score'],
            'is_positive': sentiment_data['is_positive'],
            'processed_at': datetime.now().isoformat()
        }

        logger.info(f"Analysis complete: {response['sentiment']} ({response['confidence']:.3f})")
        return jsonify(response)

    except Exception as e:
        logger.error(f"Error in sentiment analysis: {str(e)}")
        return jsonify({'error': 'Internal server error', 'details': str(e)}), 500

def process_detailed_scores(scores):
    best_score = max(scores, key=lambda x: x['score'])
    label = best_score['label'].upper()
    confidence = best_score['score']

    if 'POSITIVE' in label or label == 'LABEL_2':
        return {
            'label': 'POSITIVE',
            'confidence': confidence,
            'score': confidence,
            'is_positive': True
        }
    elif 'NEGATIVE' in label or label == 'LABEL_0':
        return {
            'label': 'NEGATIVE',
            'confidence': confidence,
            'score': -confidence,
            'is_positive': False
        }
    else:
        return {
            'label': 'NEUTRAL',
            'confidence': confidence,
            'score': 0.0,
            'is_positive': None
        }

def process_simple_score(result):
    label = result['label'].upper()
    confidence = result['score']

    if 'POSITIVE' in label:
        return {
            'label': 'POSITIVE',
            'confidence': confidence,
            'score': confidence,
            'is_positive': True
        }
    elif 'NEGATIVE' in label:
        return {
            'label': 'NEGATIVE',
            'confidence': confidence,
            'score': -confidence,
            'is_positive': False
        }
    else:
        return {
            'label': 'NEUTRAL',
            'confidence': confidence,
            'score': 0.0,
            'is_positive': None
        }

@app.route('/batch-analyze', methods=['POST'])
def batch_analyze_sentiment():
    global sentiment_analyzer, model_ready

    try:
        if not model_ready:
            return jsonify({
                'error': 'Model not ready yet. Please wait for initialization.',
                'model_loading': model_loading
            }), 503

        data = request.get_json()
        if not data or 'texts' not in data:
            return jsonify({'error': 'Missing required field: texts'}), 400

        texts = data['texts']
        if not isinstance(texts, list) or len(texts) == 0:
            return jsonify({'error': 'texts must be a non-empty array'}), 400

        if len(texts) > 100:
            return jsonify({'error': 'Batch size cannot exceed 100 texts'}), 400

        results = []
        for i, text in enumerate(texts):
            try:
                if not text or not text.strip():
                    results.append({'error': 'Empty text', 'index': i})
                    continue

                result = sentiment_analyzer(text.strip())
                if isinstance(result[0], list):
                    sentiment_data = process_detailed_scores(result[0])
                else:
                    sentiment_data = process_simple_score(result[0])

                results.append({
                    'index': i,
                    'text': text[:100] + '...' if len(text) > 100 else text,
                    'sentiment': sentiment_data['label'],
                    'confidence': sentiment_data['confidence'],
                    'score': sentiment_data['score'],
                    'is_positive': sentiment_data['is_positive']
                })
            except Exception as e:
                results.append({'error': str(e), 'index': i})

        return jsonify({
            'results': results,
            'processed_count': len([r for r in results if 'error' not in r]),
            'error_count': len([r for r in results if 'error' in r]),
            'processed_at': datetime.now().isoformat()
        })

    except Exception as e:
        logger.error(f"Error in batch analysis: {str(e)}")
        return jsonify({'error': 'Internal server error', 'details': str(e)}), 500

if __name__ == '__main__':
    initialize_model_async()
    port = int(os.environ.get('PORT', 5000))
    logger.info(f"Starting Flask server on port {port}...")
    logger.info("Model will load in background. Server is ready to accept requests.")
    app.run(host='0.0.0.0', port=port, debug=False, threaded=True)
