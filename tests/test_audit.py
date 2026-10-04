import hashlib
import json
from pathlib import Path
import sys
import unittest

ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tools'))
from audit_archive import decode_name
from inspect_containers import FormatError, Reader, unpack_sprite


class DecoderTests(unittest.TestCase):
    def test_rle_and_literals(self):
        out,_=unpack_sprite(bytes([3,0x43])+b'ABC'+bytes([0x84])+b'x',10)
        self.assertEqual(out,b'\0\0\0ABCxxxx')

    def test_overlapping_backreference(self):
        out,_=unpack_sprite(bytes([0x42])+b'ab'+bytes([0xc6,2]),8)
        self.assertEqual(out,b'abababab')

    def test_extended_lengths(self):
        self.assertEqual(unpack_sprite(bytes([0,1]),64)[0],b'\0'*64)
        self.assertEqual(unpack_sprite(bytes([0,0,0,0,0]),319)[0],b'\0'*319)

    def test_long_distance(self):
        src=bytes([0x80,193,65,0xc1,0,0,0])
        self.assertEqual(unpack_sprite(src,257)[0],b'A'*257)

    def test_truncated_literal_is_error(self):
        with self.assertRaises(FormatError):unpack_sprite(b'\x43A',3)

    def test_backreference_before_start_is_error(self):
        with self.assertRaises(FormatError):unpack_sprite(b'\xc2\x01',2)

    def test_output_overrun_is_error(self):
        with self.assertRaises(FormatError):unpack_sprite(b'\x04',3)

    def test_short_output_is_error(self):
        with self.assertRaises(FormatError):unpack_sprite(b'\x02',3)

    def test_reader_bounds(self):
        with self.assertRaises(FormatError):Reader(b'\0').u32()


class ArchiveTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest=json.loads((ROOT/'research/filename_manifest.json').read_text())
        cls.summary=json.loads((ROOT/'research/container_summary.json').read_text())

    def test_every_entry_hash_preserved(self):
        for e in self.manifest['entries']:
            if e['stored_path']:
                with self.subTest(entry=e['entry_index']):
                    b=(ROOT/e['stored_path']).read_bytes()
                    self.assertEqual(len(b),e['bytes'])
                    self.assertEqual(hashlib.sha256(b).hexdigest(),e['sha256'])

    def test_all_names_decoded(self):
        for e in self.manifest['entries']:
            with self.subTest(entry=e['entry_index']):
                self.assertIsNone(e['decode_error'])
                decoded,_,_=decode_name(bytes.fromhex(e['raw_zip_name_hex']),0)
                self.assertEqual(decoded,e['decoded_japanese_path'])

    def test_all_unique_containers_covered(self):
        expected={e['sha256'] for e in self.manifest['entries'] if e['stored_path'] and not e['is_metadata'] and Path(e['stored_path']).suffix in ('.kgt','.demo','.player','.stage')}
        self.assertEqual(self.summary['errors'],[])
        actual={json.loads(p.read_text())['source_sha256'] for p in (ROOT/'research/containers').glob('*.json')}
        self.assertEqual(actual,expected)
        self.assertEqual(len(actual),46)

    def test_kgt_filename_references_exist(self):
        k=json.loads(next((ROOT/'research/containers').glob('*.kgt.json')).read_text())
        names={e['decoded_japanese_path'].rsplit('/',1)[-1] for e in self.manifest['entries'] if not e['is_metadata']}
        for field,ext in [('characters','.player'),('stages','.stage'),('demos','.demo')]:
            for name in k['specific'][field]:
                if name:
                    with self.subTest(reference=name+ext):self.assertIn(name+ext,names)

    def test_story_scene_and_stage_references_exist(self):
        k=json.loads(next((ROOT/'research/containers').glob('*.kgt.json')).read_text())
        for p in (ROOT/'research/containers').glob('*.player.json'):
            j=json.loads(p.read_text())
            for e in j['specific']['story_slots']:
                with self.subTest(player=j['title'],slot=e['slot']):
                    if e['type']==2:
                        n=e['demo_index'];self.assertGreater(n,0);self.assertTrue(k['specific']['demos'][n-1])
                    elif e['type']==1 and e['stage_index']:
                        self.assertTrue(k['specific']['stages'][e['stage_index']-1])

    def test_demo_and_stage_trailers_are_zero(self):
        for e in self.summary['parsed']:
            if e['extension'] in ('.demo','.stage'):
                self.assertEqual(e['trailing_nonzero'],0)


if __name__=='__main__':unittest.main()
