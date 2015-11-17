#! /usr/bin/env python3
# -*- coding: utf-8 -*-

from time import time
import sys,re,argparse,struct
import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_extraction.text import HashingVectorizer
from sklearn.feature_extraction.text import TfidfTransformer
from sklearn import (manifold, datasets, decomposition, ensemble, lda,
                     random_projection)
from array import array
import itertools
#----------------------------------------------------------------------

class EmojiModel:

  def __init__(self):
    self.matrix = dict()
    self.docs = list()
    self.emoji_list = list()
    self.words = list()
  def load_file(self, afile):
    with open(afile, 'r') as f:  
      for line in f:
        (emoji, *text) = re.split('\s+', line.strip())
        #emoji_dict[emoji] = ' '.join(text)
        self.docs.append(' '.join(text))
        self.emoji_list.append(emoji)

    tfidf = TfidfVectorizer(stop_words='english')
    idfmatrix = tfidf.fit_transform(self.docs)
    idfList = tfidf.idf_
    self.words = tfidf.get_feature_names()
    #print (self.words)
    #  print (emoji_list[docid], ":", words[wid], "=", idfList[wid])
    rows,cols = idfmatrix.nonzero()
    for docid,wid in zip(rows,cols):
      if wid in self.matrix:
        tp = self.matrix[wid]
        tp.append((docid, idfmatrix[docid,wid]))
      else:
        self.matrix[wid] = [(docid, idfmatrix[docid,wid])]

  def write_model(self, out_file):
    n_bytes = 0
    #first write vocab
    with open(out_file, "wb") as f:
      vocab_bytes = bytearray(' '.join(self.words), 'utf-8')
      n = len(vocab_bytes)
      print("vocab bytes=%d",n)
      '''
      write vocab, first 2-byte (unsigned int) indicates how many following bytes to
      reach the end of vocab string (space separated)
      '''
      fmt = '<H'+str(n)+'s'
      data_vocab = struct.pack(fmt, n, vocab_bytes )
      f.write(data_vocab)
      n_bytes +=struct.calcsize(fmt)
      print("vocab info: %d bytes"% struct.calcsize(fmt))
      '''
      write emoji string, utf8 encoded. first 2-byte indicates how many following bytes to
      reach the end
      '''
      emoji_str = ' '.join(self.emoji_list)
      print(emoji_str)
      emoji_bytes = bytearray(emoji_str, 'utf-8')
      n = len(emoji_bytes)
      fmt = '<H'+str(n)+'s'
      data_emoji = struct.pack(fmt, n, emoji_bytes )
      f.write(data_emoji)
      n_bytes +=struct.calcsize(fmt)
      print("emoji info: %d bytes"% struct.calcsize(fmt))

      '''
      write mappings 
      '''
      total_bytes = 0;
      for wid in self.matrix.keys():
        tu = self.matrix[wid]
        #how many (emoji, value) pairs
        n = len(tu)
        fmt = "<HH"+n*"Hf"
        total_bytes += struct.calcsize(fmt)
        data_m = struct.pack(fmt, wid, n, *list(itertools.chain(*tu)) )
        f.write(data_m)
      print("mapping bytes:%d" % (total_bytes))
      n_bytes +=total_bytes
      print("total size of model:%d bytes"%n_bytes)

if __name__ == "__main__":
    
    parser = argparse.ArgumentParser(description='build emoji prediction model',
                                  formatter_class=argparse.ArgumentDefaultsHelpFormatter)
    parser.add_argument('input_file', help='input file, each line of "emoji" "words sep by words" ')
    parser.add_argument('output_file',help='output model file')

    args = parser.parse_args()

    m = EmojiModel()
    m.load_file(args.input_file)
    m.write_model(args.output_file)
