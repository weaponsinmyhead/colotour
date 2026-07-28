package domain

import (
	"errors"
	"strings"
)

type PlanItineraryRequest struct {
	Destination  string     `json:"destination"`
	Center       *GeoPoint  `json:"center,omitempty"`
	Origin       *GeoPoint  `json:"origin,omitempty"`
	OriginName   string     `json:"originName,omitempty"`
	Interests    []Category `json:"interests"`
	Mobility     []string   `json:"mobility"`
	StartMinutes int        `json:"startMinutes"`
	EndMinutes   int        `json:"endMinutes"`
	People       int        `json:"people"`
	Budget       PriceLevel `json:"budget"`
	Pace         TravelPace `json:"pace,omitempty"`
	IncludeFood  bool       `json:"includeFood"`
}

func (request PlanItineraryRequest) Validate() error {
	if strings.TrimSpace(request.Destination) == "" {
		return errors.New("destination is required")
	}
	if request.Center != nil {
		if err := request.Center.Validate(); err != nil {
			return err
		}
	}
	if request.Origin != nil {
		if err := request.Origin.Validate(); err != nil {
			return err
		}
	}
	if len(request.Interests) == 0 {
		return errors.New("at least one interest is required")
	}
	for _, interest := range request.Interests {
		if !interest.Valid() {
			return errors.New("unsupported interest")
		}
	}
	if len(request.Mobility) == 0 {
		return errors.New("at least one mobility option is required")
	}
	if request.StartMinutes < 0 || request.EndMinutes > 24*60 {
		return errors.New("time range must be within one day")
	}
	if request.EndMinutes-request.StartMinutes < 120 {
		return errors.New("time range must be at least two hours")
	}
	if request.People < 1 || request.People > 20 {
		return errors.New("people must be between 1 and 20")
	}
	if !request.Budget.Valid() {
		return errors.New("unsupported budget")
	}
	if request.Pace != "" && !request.Pace.Valid() {
		return errors.New("unsupported pace")
	}
	return nil
}

type TravelPace string

const (
	PaceRelaxed  TravelPace = "relaxed"
	PaceBalanced TravelPace = "balanced"
	PaceIntense  TravelPace = "intense"
)

func (pace TravelPace) Valid() bool {
	switch pace {
	case PaceRelaxed, PaceBalanced, PaceIntense:
		return true
	default:
		return false
	}
}

type ItineraryStop struct {
	Order           int      `json:"order"`
	PlaceID         string   `json:"placeId"`
	Type            string   `json:"type"`
	Title           string   `json:"title"`
	Summary         string   `json:"summary"`
	Location        GeoPoint `json:"location"`
	StartsAtMinutes int      `json:"startsAtMinutes"`
	DurationMinutes int      `json:"durationMinutes"`
	EstimatedCost   *Money   `json:"estimatedCost,omitempty"`
	Category        Category `json:"category"`
	ImageURL        string   `json:"imageUrl,omitempty"`
	Reason          string   `json:"reason"`
}

type PlannedItinerary struct {
	Destination       string          `json:"destination"`
	Center            GeoPoint        `json:"center"`
	Origin            GeoPoint        `json:"origin"`
	Stops             []ItineraryStop `json:"stops"`
	StartMinutes      int             `json:"startMinutes"`
	EndMinutes        int             `json:"endMinutes"`
	EstimatedCost     *Money          `json:"estimatedCost,omitempty"`
	DataSourceSummary string          `json:"dataSourceSummary"`
}
