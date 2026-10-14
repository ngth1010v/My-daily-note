"""Builds core/data/src/main/assets/autostyle/embeddings.bin, the on-device text embedding model
used to auto-pick a new action's color and icon.

Source: minishlab/potion-multilingual-128M (MIT), a Model2Vec static model distilled from BAAI/bge-m3.
The app only needs English and Vietnamese, so the 500k-piece vocabulary is cut to the pieces those
languages use, projected to fewer dimensions and stored as int8 rows.

Usage (Python 3.10+, `pip install numpy safetensors tokenizers`):
    python build_model.py <model dir> <en word list> <vi word list> <EntryIcons.kt> <out .bin> [--dim 128]
  <model dir>: model.safetensors + tokenizer.json from huggingface.co/minishlab/potion-multilingual-128M
  word lists: one "word count" per line, e.g. FrequencyWords en_50k.txt / vi_50k.txt (OpenSubtitles 2018)

File format (big-endian):
    "MDN1", u32 pieces, u16 dims, u32 unk_id, f32 unk_score
    per piece: u8 byte length, UTF-8 bytes, f32 unigram score, f32 row scale
    i8[pieces * dims] rows (row value = int8 * row scale)
"""
import argparse
import json
import re
import struct
import sys

import numpy as np
from safetensors.numpy import load_file
from tokenizers import Tokenizer

LETTERS = re.compile(r"[0-9a-zà-ỹđ]", re.IGNORECASE)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('model')
    ap.add_argument('en')
    ap.add_argument('vi')
    ap.add_argument('icons')
    ap.add_argument('out')
    ap.add_argument('--dim', type=int, default=128)
    a = ap.parse_args()

    tok = Tokenizer.from_file(a.model + '/tokenizer.json')
    with open(a.model + '/tokenizer.json', encoding='utf-8') as f:
        vocab = json.load(f)['model']['vocab']
    emb = next(iter(load_file(a.model + '/model.safetensors').values())).astype(np.float32)

    texts = []
    for path in (a.en, a.vi):
        with open(path, encoding='utf-8') as f:
            texts += [line.split(' ')[0] for line in f if line.strip()]
    with open(a.icons, encoding='utf-8') as f:
        texts += re.findall(r'"([^"\n]+)"', f.read())

    keep = {1}  # [UNK]
    for t in texts:
        keep.update(tok.encode(t.lower(), add_special_tokens=False).ids)
    # Single characters (with and without the word marker) so any en/vi input still segments.
    for i, (piece, _) in enumerate(vocab):
        p = piece[1:] if piece.startswith('▁') else piece
        if len(p) <= 1 and (not p or LETTERS.match(p) or ord(p) < 128):
            keep.add(i)
    keep = sorted(keep)
    rows = emb[keep]

    # Uncentered PCA keeps dot products (and the per-piece norms Model2Vec uses as weights).
    _, _, vt = np.linalg.svd(rows, full_matrices=False)
    proj = rows @ vt[:a.dim].T
    scale = np.abs(proj).max(1) / 127 + 1e-12
    q = np.round(proj / scale[:, None]).astype(np.int8)

    unk_new = keep.index(1)
    with open(a.out, 'wb') as f:
        f.write(b'MDN1' + struct.pack('>IHIf', len(keep), a.dim, unk_new, min(s for _, s in vocab) - 10))
        for i, s in zip(keep, scale):
            b = vocab[i][0].encode('utf-8')
            assert len(b) < 256, vocab[i][0]
            f.write(struct.pack('>B', len(b)) + b + struct.pack('>ff', vocab[i][1], s))
        f.write(q.tobytes())
    print(f'{len(keep)} pieces x {a.dim} dims -> {a.out}', file=sys.stderr)


if __name__ == '__main__':
    main()
