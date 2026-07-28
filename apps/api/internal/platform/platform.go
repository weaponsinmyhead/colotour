package platform

import (
	"crypto/rand"
	"encoding/hex"
	"time"
)

type RealClock struct{}

func (RealClock) Now() time.Time {
	return time.Now().UTC()
}

type RandomIDGenerator struct{}

func (RandomIDGenerator) NewID() string {
	var raw [16]byte
	if _, err := rand.Read(raw[:]); err != nil {
		// A timestamp fallback keeps the API operational if the OS entropy source
		// is temporarily unavailable; uniqueness is still sufficient for this MVP.
		return "wf-" + time.Now().UTC().Format("20060102150405.000000000")
	}
	raw[6] = (raw[6] & 0x0f) | 0x40
	raw[8] = (raw[8] & 0x3f) | 0x80
	return hex.EncodeToString(raw[0:4]) + "-" +
		hex.EncodeToString(raw[4:6]) + "-" +
		hex.EncodeToString(raw[6:8]) + "-" +
		hex.EncodeToString(raw[8:10]) + "-" +
		hex.EncodeToString(raw[10:16])
}
