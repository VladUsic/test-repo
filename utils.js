// @flow

import { Platform } from 'react-native';

import type {
  CardParameters,
  AndroidCardParameters,
  IOSCardParameters,
} from './types';

export function mapParameters(
  parameters: CardParameters
): AndroidCardParameters | IOSCardParameters {
  if (Platform.OS === 'android') {
    return parameters;
  }

  // iOS field mapping
  // https://github.com/braintree/braintree_ios/blob/master/BraintreeCard/BTCard.m#L14
  return {
    number: parameters.number,
    cvv: parameters.cvv,
    expiration_date: parameters.expirationDate,
    givenName: parameters.givenName,
    surname: parameters.surname,
    cardholder_name: parameters.cardholderName,
    billingAddress: {
      postalCode: parameters.postalCode,
      streetAddress: parameters.streetAddress,
      extendedAddress: parameters.extendedAddress,
      locality: parameters.locality,
      region: parameters.region,
      countryName: parameters.countryName,
      countryCodeAlpha2: parameters.countryCodeAlpha2,
      countryCodeAlpha3: parameters.countryCodeAlpha3,
      countryCodeNumeric: parameters.countryCodeNumeric,
      firstName: parameters.firstName,
      lastName: parameters.lastName,
      company: parameters.company,
    },
  };
}
