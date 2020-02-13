/* AUTOMATICALLY GENERATED FILE - DO NOT EDIT */

#ifndef JFRFILES_JFREVENTCLASSES_HPP
#define JFRFILES_JFREVENTCLASSES_HPP

#include "oops/klass.hpp"
#include "jfrfiles/jfrTypes.hpp"
#include "jfr/utilities/jfrTypes.hpp"
#include "utilities/macros.hpp"
#include "utilities/ticks.hpp"
#if INCLUDE_JFR
#include "jfr/recorder/service/jfrEvent.hpp"
/*
 * Each event class has an assert member function verify() which is invoked
 * just before the engine writes the event and its fields to the data stream.
 * The purpose of verify() is to ensure that all fields in the event are initialized
 * and set before attempting to commit.
 *
 * We enforce this requirement because events are generally stack allocated and therefore
 * *not* initialized to default values. This prevents us from inadvertently committing
 * uninitialized values to the data stream.
 *
 * The assert message contains both the index (zero based) as well as the name of the field.
 */

class EventTestArrayField : public JfrEvent<EventTestArrayField>
{
 private:
  unsigned _firstField;
  const Klass* _secondField;
  unsigned* _thirdField;
  uint _thirdField_length;
  const Klass** _fourthFiekd;
  uint _fourthFiekd_length;

 public:
  static const bool hasThread = false;
  static const bool hasStackTrace = false;
  static const bool isInstant = false;
  static const bool hasCutoff = false;
  static const bool isRequestable = false;
  static const JfrEventId eventId = JfrTestArrayFieldEvent;

  EventTestArrayField(EventStartTime timing=TIMED) : JfrEvent<EventTestArrayField>(timing) {}

  void set_firstField(unsigned new_value) {
    this->_firstField = new_value;
    DEBUG_ONLY(set_field_bit(0));
  }
  void set_secondField(const Klass* new_value) {
    this->_secondField = new_value;
    DEBUG_ONLY(set_field_bit(1));
  }
  void set_thirdField(unsigned* new_value, uint new_value_length) {
    this->_thirdField_length = new_value_length;
    this->_thirdField = new_value;
    DEBUG_ONLY(set_field_bit(2));
  }
  void set_fourthFiekd(const Klass** new_value, uint new_value_length) {
    this->_fourthFiekd_length = new_value_length;
    this->_fourthFiekd = new_value;
    DEBUG_ONLY(set_field_bit(3));
  }

  template <typename Writer>
  void writeData(Writer& w) {
    w.write(_firstField);
    w.write(_secondField);
    w.write(_thirdField_length);
    for (uint __index = 0; __index < _thirdField_length; __index++) {
        w.write(_thirdField[__index]);
    }
    w.write(_fourthFiekd_length);
    for (uint __index = 0; __index < _fourthFiekd_length; __index++) {
        w.write(_fourthFiekd[__index]);
    }
  }

  using JfrEvent<EventTestArrayField>::commit; // else commit() is hidden by overloaded versions in this class

  EventTestArrayField(
    unsigned firstField,
    const Klass* secondField,
    unsigned* thirdField,
    uint thirdField_length,
    const Klass** fourthFiekd,
    uint fourthFiekd_length) : JfrEvent<EventTestArrayField>(TIMED) {
    if (should_commit()) {
      set_firstField(firstField);
      set_secondField(secondField);
      set_thirdField(thirdField, thirdField_length);
      set_fourthFiekd(fourthFiekd, fourthFiekd_length);
    }
  }

  void commit(unsigned firstField,
              const Klass* secondField,
              unsigned* thirdField,
              uint thirdField_length,
              const Klass** fourthFiekd,
              uint fourthFiekd_length) {
    if (should_commit()) {
      set_firstField(firstField);
      set_secondField(secondField);
      set_thirdField(thirdField, thirdField_length);
      set_fourthFiekd(fourthFiekd, fourthFiekd_length);
      commit();
    }
  }

  static void commit(const Ticks& startTicks,
                     const Ticks& endTicks,
                     unsigned firstField,
                     const Klass* secondField,
                     unsigned* thirdField,
                     uint thirdField_length,
                     const Klass** fourthFiekd,
                     uint fourthFiekd_length) {
    EventTestArrayField me(UNTIMED);

    if (me.should_commit()) {
      me.set_starttime(startTicks);
      me.set_endtime(endTicks);
      me.set_firstField(firstField);
      me.set_secondField(secondField);
      me.set_thirdField(thirdField, thirdField_length);
      me.set_fourthFiekd(fourthFiekd, fourthFiekd_length);
      me.commit();
    }
  }

#ifdef ASSERT
  void verify() const {
    assert(verify_field_bit(0), "Attempting to write an uninitialized event field: %s", "_firstField");
    assert(verify_field_bit(1), "Attempting to write an uninitialized event field: %s", "_secondField");
    assert(verify_field_bit(2), "Attempting to write an uninitialized event field: %s", "_thirdField");
    assert(verify_field_bit(3), "Attempting to write an uninitialized event field: %s", "_fourthFiekd");
  }
#endif
};



#else // !INCLUDE_JFR

class JfrEvent {
 public:
  JfrEvent() {}
  void set_starttime(const Ticks&) const {}
  void set_endtime(const Ticks&) const {}
  bool should_commit() const { return false; }
  static bool is_enabled() { return false; }
  void commit() {}
};

class EventTestArrayField : public JfrEvent
{
 public:
  EventTestArrayField(EventStartTime ignore=TIMED) {}
  EventTestArrayField(
    unsigned,
    const Klass*,
    unsigned*,
    uint,
    const Klass**,
    uint) { }
  void set_firstField(unsigned) { }
  void set_secondField(const Klass*) { }
  void set_thirdField(unsigned*, uint) { }
  void set_fourthFiekd(const Klass**, uint) { }
};



#endif // INCLUDE_JFR
#endif // JFRFILES_JFREVENTCLASSES_HPP
