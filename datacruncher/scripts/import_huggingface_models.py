import logging as log

# -*- coding: utf-8 -*-
"""
    Spark NLP - Export DistilBert from Huggingface.ipynb

    Automatically generated by Colaboratory.

    Original file is located at
        https://colab.research.google.com/drive/1LtkNkSVCFoItq4vEHsR8cD4XvE7HCHAG

    [![Open In Colab](https://colab.research.google.com/assets/colab-badge.svg)](https://colab.research.google.com/github/JohnSnowLabs/spark-nlp-workshop/blob/master/jupyter/transformers/HuggingFace%20in%20Spark%20NLP%20-%20DistilBertForTokenClassification.ipynb)

    ## Import DistilBertForTokenClassification models from HuggingFace 🤗  into Spark NLP 🚀 

    Let's keep in mind a few things before we start 😊 

    - This feature is only in `Spark NLP 3.2.x` and after. So please make sure you have upgraded to the latest Spark NLP release
    - You can import BERT models trained/fine-tuned for token classification via `BertForTokenClassification` or `TFBertForTokenClassification`. These models are usually under `Token Classification` category and have `bert` in their labels
    - Reference: [TFDistilBertForTokenClassification](https://huggingface.co/transformers/model_doc/distilbert.html#tfdistilbertfortokenclassification)
    - Some [example models](https://huggingface.co/models?filter=distilbert&pipeline_tag=token-classification)

    ## Export and Save HuggingFace model

    - Let's install `HuggingFace` and `TensorFlow`. You don't need `TensorFlow` to be installed for Spark NLP, however, we need it to load and save models from HuggingFace.
    - We lock TensorFlow on `2.4.1` version and Transformers on `4.8.1`. 
    This doesn't mean it won't work with the future releases, but we wanted you to know which versions have been tested successfully.
    

    #  !pip install -q transformers==4.8.1 tensorflow==2.4.1
    # https://colab.research.google.com/github/JohnSnowLabs/spark-nlp-workshop/blob/master/jupyter/transformers/HuggingFace%20in%20Spark%20NLP%20-%20DistilBertForTokenClassification.ipynb#scrollTo=OYnT5U8N9dxT

    - HuggingFace comes with a native `saved_model` feature inside `save_pretrained` function for TensorFlow based models. We will use that to save it as TF `SavedModel`.
    - We'll use [elastic/distilbert-base-cased-finetuned-conll03-english](elastic/distilbert-base-cased-finetuned-conll03-english) model from HuggingFace as an example
    - In addition to `TFDistilBertForTokenClassification` we also need to save the `DistilBertTokenizer`. This is the same for every model, these are assets needed for tokenization inside Spark NLP.
"""
from transformers import *
from shutil import *

def format_model_dirs(model_name: str, model):

    # !ls -l {MODEL_NAME}

    # !ls -l {MODEL_NAME}_tokenizer

    """- As you can see, we need the SavedModel from `saved_model/1/` path
    - We also be needing `vocab.txt` from the tokenizer
    - All we need is to just copy the `vocab.txt` to `saved_model/1/assets` which Spark NLP will look for
    - In addition to vocabs, we also need `labels` and their `ids` which is saved inside the model's config. We will save this inside `labels.txt`
    """

    asset_path = f'models/{model_name}/saved_model/1/assets'
    # !cp {MODEL_NAME}_tokenizer/vocab.txt {asset_path}/
    copyfile(f"models/{model_name}_tokenizer/vocab.txt", f"{asset_path}/vocab.txt")

    # get label2id dictionary 
    labels = model.config.label2id
    # sort the dictionary based on the id
    labels = sorted(labels, key=labels.get)

    with open(asset_path+'/labels.txt', 'w+') as f:
        f.write('\n'.join(labels))

    """Voila! We have our `vocab.txt` and `labels.txt` inside assets directory"""

    # !ls -l {MODEL_NAME}/assets
    # !zip -r model.zip {MODEL_NAME}
    # !ls

def download_distillbert(model_name: str):
    # ! rm -rf {MODEL_NAME}
    tokenizer = DistilBertTokenizer.from_pretrained(model_name)
    tokenizer.save_pretrained('models/{}_tokenizer/'.format(model_name))

    # Add from_pt=True since there is no TF weights available for this model
    # from_pt=True will convert the pytorch model to tf model

    model = TFDistilBertForSequenceClassification.from_pretrained(model_name, from_pt=True)
    model.save_pretrained("models/{}".format(model_name), saved_model=True)

    format_model_dirs(model_name, model)

def download_bert(model_name: str):
    tokenizer = BertTokenizer.from_pretrained(model_name)
    tokenizer.save_pretrained('models/{}_tokenizer/'.format(model_name))

    model = TFBertForSequenceClassification.from_pretrained(model_name, from_pt=True)
    model.save_pretrained("models/{}".format(model_name), saved_model=True)

    format_model_dirs(model_name, model)

def download_roberta(model_name: str, for_seq: bool = False):
    """
    use for_seq=True for sentiment model
    """
    # tokenizer = RobertaTokenizer.from_pretrained(model_name)
    tokenizer = AutoTokenizer.from_pretrained(model_name)
    tokenizer.save_pretrained('./{}_tokenizer/'.format(model_name))

    if for_seq:
        model = TFRobertaForSequenceClassification.from_pretrained(model_name, from_pt=True)
    else:
        model = TFRobertaForTokenClassification.from_pretrained(model_name, from_pt=True)
    model.save_pretrained("./{}".format(model_name), saved_model=True)

    asset_path = f'{model_name}/saved_model/1/assets'

    with open(f'{asset_path}/vocab.txt', 'w') as f:
        for item in tokenizer.get_vocab().keys():
            f.write("%s\n" % item)

    # get label2id dictionary 
    labels = model.config.label2id
    # sort the dictionary based on the id
    labels = sorted(labels, key=labels.get)

    with open(f'{asset_path}/labels.txt', 'w') as f:
        f.write('\n'.join(labels))
    
    copyfile(f"{model_name}_tokenizer/merges.txt", f"{asset_path}/merges.txt")

def download_xmlroberta(model_name: str):
    tokenizer = XLMRobertaTokenizer.from_pretrained(model_name)
    tokenizer.save_pretrained(f'./{model_name}_tokenizer/')

    # just in case if there is no TF/Keras file provided in the model
    # we can just use `from_pt` and convert PyTorch to TensorFlow
    try:
        print('try downloading TF weights')
        model = TFXLMRobertaForTokenClassification.from_pretrained(model_name)
    except:
        print('try downloading PyTorch weights')
        model = TFXLMRobertaForTokenClassification.from_pretrained(model_name, from_pt=True)

    model.save_pretrained(f"./{model_name}", saved_model=True)

    asset_path = f'{model_name}/saved_model/1/assets'
    # let's copy sentencepiece.bpe.model file to saved_model/1/assets
    copyfile(f"{model_name}_tokenizer/sentencepiece.bpe.model", f"{asset_path}/sentencepiece.bpe.model")
    # get label2id dictionary 
    labels = model.config.label2id
    # sort the dictionary based on the id
    labels = sorted(labels, key=labels.get)

    with open(f'{asset_path}/labels.txt', 'w') as f:
        f.write('\n'.join(labels))

if __name__ == "__main__":
    model_name = "cardiffnlp/twitter-roberta-base-sentiment"
    # download_distillbert(model_name)
    # download_xmlroberta(model_name)
    # download_bert(model_name)
    download_roberta(model_name, for_seq=True)
    # move(curr_dir, resources_dir)

    # TODO move the model downloaded in current dir to desired dir